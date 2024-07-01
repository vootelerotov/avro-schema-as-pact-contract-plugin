plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("application")
}

group = "io.github.vootelerotov"
version = "0.0.3"

repositories {
    mavenCentral()
}

val grpcVersion = "1.65.0"

dependencies {
    implementation("io.github.oshai:kotlin-logging:7.0.0")

    implementation("org.apache.avro:avro:1.11.3")

    implementation("io.pact.plugin.driver:core:0.4.2")

    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "io.github.vootelerotov.avro.schema.contract.PluginAppKt"
}



distributions {
    main {
        distributionBaseName = "avro-schema-as-pact-contract"
        contents {
            from("${project.layout.projectDirectory}/pact-plugin.json") {
                into("")
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

