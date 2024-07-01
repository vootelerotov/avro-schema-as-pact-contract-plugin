plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("application")
}

group = "io.github.vootelerotov"
version = "0.0.2"

repositories {
    mavenCentral()
}

val grpcVersion = "1.62.2"

dependencies {
    implementation("io.github.oshai:kotlin-logging:6.0.3")

    implementation("org.apache.avro:avro:1.11.3")

    implementation("io.pact.plugin.driver:core:0.4.2")

    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

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

