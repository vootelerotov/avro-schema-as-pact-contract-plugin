package io.github.vootelerotov.avro.schema.contract

import com.google.protobuf.BytesValue
import com.google.protobuf.Empty
import com.google.protobuf.Struct
import com.google.protobuf.Value
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vootelerotov.avro.schema.contract.ParseResult.Success
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status.INVALID_ARGUMENT
import io.grpc.stub.StreamObserver
import io.pact.plugin.PactPluginGrpc
import io.pact.plugin.Plugin
import io.pact.plugin.Plugin.Body
import io.pact.plugin.Plugin.Catalogue
import io.pact.plugin.Plugin.CatalogueEntry.EntryType.CONTENT_MATCHER
import io.pact.plugin.Plugin.CompareContentsResponse
import io.pact.plugin.Plugin.ConfigureInteractionResponse
import io.pact.plugin.Plugin.InteractionResponse
import io.pact.plugin.Plugin.PluginConfiguration
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun main() {
  val server: Server = ServerBuilder.forPort(0).addService(PactPluginService()).build()
  Runtime.getRuntime().addShutdownHook(Thread {
    logger.info { "Shutting down gRPC server since JVM is shutting down" }
    server.shutdownNow()
    logger.info { "Server shut down" }
  })

  server.start()

  logger.info { "Server started, listening on ${server.port}" }
  println("{ \"port\":${server.port}, \"serverKey\":\"${UUID.randomUUID()}\" }")
  server.awaitTermination()
}

class PactPluginService: PactPluginGrpc.PactPluginImplBase() {

  override fun initPlugin(
    request: Plugin.InitPluginRequest,
    responseObserver: StreamObserver<Plugin.InitPluginResponse>,
  ) {
    logger.info { "Init request from ${request.implementation}/${request.version}" }
    val response = Plugin.InitPluginResponse.newBuilder().apply {
      logger.info { "Registering 'application/avro-message-schema-json' as supported contet type " }
      this.addCatalogueBuilder()
        .setType(CONTENT_MATCHER)
        .setKey("avro-schema-as-contract")
        .putValues("content-types", "application/avro-message-schema-json")
    }.build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun updateCatalogue(request: Catalogue, responseObserver: StreamObserver<Empty>) {
    logger.info { "Update catalogue request, doing nothing" }
    responseObserver.onNext(Empty.newBuilder().build())
    responseObserver.onCompleted()
  }

  override fun configureInteraction(
    request: Plugin.ConfigureInteractionRequest,
    responseObserver: StreamObserver<ConfigureInteractionResponse>,
  ) {
    logger.info { "Configure interaction request for ${request.contentType}" }
    val config = request.contentsConfig.fieldsMap

    val rawSchema = config["schema"]
    if (rawSchema == null) {
      logger.error { "Schema not provided" }
      responseObserver.onError(INVALID_ARGUMENT.withDescription("Schema not provided").asException())
      return
    }

    when(parseSchema(rawSchema)) {
      is ParseResult.Failure -> {
        val (error, exception) = parseSchema(rawSchema) as ParseResult.Failure
        logger.error(exception) { error }
        responseObserver.onError(INVALID_ARGUMENT.withDescription(error).asException())
        return
      }
      else -> {}
    }

    val content = config["content"]

    if (content == null) {
      logger.error { "Content not provided" }
      responseObserver.onError(INVALID_ARGUMENT.withDescription("Content not provided").asException())
      return
    }

    val interactionResponse = InteractionResponse.newBuilder()
      .setContents(Body.newBuilder()
        .setContent(BytesValue.newBuilder().setValue(content.stringValueBytes))
        .setContentType("application/avro-message-schema-json")
      )
      .setPluginConfiguration(PluginConfiguration.newBuilder()
        .setPactConfiguration(Struct.newBuilder().putFields("plugin", stringValue("avro-schema-as-contract")))
        .setInteractionConfiguration(Struct.newBuilder().putFields("schema", rawSchema).build())
        .build()
      ).build()

    responseObserver.onNext(ConfigureInteractionResponse.newBuilder().addInteraction(interactionResponse).build())
    responseObserver.onCompleted()
  }

  override fun compareContents(
    request: Plugin.CompareContentsRequest,
    responseObserver: StreamObserver<CompareContentsResponse>,
  ) {
    val rawSchema = request.pluginConfiguration.interactionConfiguration.fieldsMap["schema"]
    if (rawSchema == null) {
      logger.error { "Schema not provided" }
      responseObserver.onError(INVALID_ARGUMENT.withDescription("Schema not provided").asException())
      return
    }

    val actualMessage = request.actual.content.value.toString(Charsets.UTF_8);

    val schema = when(val parsedSchema = parseSchema(rawSchema)) {
      is ParseResult.Failure -> {
        logger.error(parsedSchema.exception) { parsedSchema.error }
        responseObserver.onError(INVALID_ARGUMENT.withDescription(parsedSchema.error).asException())
        return
      }
      is Success -> parsedSchema.schema
    }

    val reader = GenericDatumReader<GenericRecord>(schema)

    try {
      val decoder = DecoderFactory.get().jsonDecoder(schema, actualMessage)
      reader.read(null, decoder)
      responseObserver.onNext(CompareContentsResponse.newBuilder().build())
      responseObserver.onCompleted()
    }
    catch (e: Exception) {
      logger.error(e) { "Failed to parse message with schema" }
      responseObserver.onError(
        INVALID_ARGUMENT.withDescription("Failed to parse message with schema: ${e.message}").asException()
      )
      return
    }

  }

  private fun parseSchema(rawSchema: Value): ParseResult = try {
      Success(Schema.Parser().parse(rawSchema.stringValue))
  } catch (e: Exception) {
      ParseResult.Failure("Failed to parse schema", e)
  }

}

sealed interface ParseResult {
  data class Success(val schema: Schema) : ParseResult
  data class Failure(val error: String, val exception: Exception) : ParseResult
}

fun stringValue(value: String): Value = Value.newBuilder().setStringValue(value).build()
