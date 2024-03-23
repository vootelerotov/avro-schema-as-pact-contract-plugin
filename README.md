# Pact plugin: AVRO schema as a Pact contract

The plugin allows to generate a Pact contract from an AVRO schema.

## Motivation

One approach to AVRO schemas is to define them independently on the producers and consumers.
This allows consumers to declare a subset of the schema that they are interested in. 

And then use the AVRO bindings generated from that schema to serialize and deserialize the data.

This makes sure that producer can evolve the schema, also in backward incompatible ways,
without breaking the consumers in case they do not care about the affected fields.

The plugin allows to generate a Pact contract from the consumer side AVRO schema. 

The contract is then that the producer will send messages that can be deserialized using the consumer-side schema(s).

## Usage

Make sure you have the Pact plugin installed in your project. On the consider side, create the pact as follow:
```java
V4Pact pact = new PactBuilder(consumer, provider, V4)
        .usingPlugin("avro-schema-as-pact-contract")
        .expectsToReceive(pulsarConsumerAnnotation.topic(), "core/interaction/message")
        .with(Map.of(
            "message.contents", Map.of(
                "schema", messageBody.getSchema().toString(true),
                "content", messageBody, // Not used as part of the pact, but useful for debugging
                "pact:content-type", "application/avro-message-schema-json"
            )
        )).toPact();
```

The plugin will generate a Pact contract from the AVRO schema and the message content.

On the producer side, have the plugin installed and create MessagePact as usual.

## License
MIT License
