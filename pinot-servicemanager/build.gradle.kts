plugins {
  `java-library`
  id("org.hypertrace.docker-java-application-plugin") version "0.7.1" apply true
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot-servicemanager")
  }
}

dependencies {
  implementation("org.apache.pinot:pinot-spi:0.5.0")
  implementation("org.hypertrace.core.kafkastreams.framework:kafka-streams-serdes:0.1.10")
}


