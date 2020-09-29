plugins {
  `java-library`
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot-servicemanager")
    dockerFile.set(file("Dockerfile"))
  }
}

dependencies {
  implementation("org.apache.pinot:pinot-spi:0.5.0")
  implementation("org.apache.pinot:pinot-avro-base:0.5.0")
  implementation("org.hypertrace.core.kafkastreams.framework:kafka-streams-serdes:0.1.11")
}


