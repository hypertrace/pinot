plugins {
  `java-library`
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot-servicemanager")
    tasks.named(buildTaskName) {
      dependsOn("copyPlugins")
    }
  }
}

configure<JavaPluginExtension> {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

val plugins by configurations.creating

dependencies {
  compileOnly("org.apache.pinot:pinot-spi:0.5.0")
  compileOnly("org.apache.pinot:pinot-avro-base:0.5.0")
  compileOnly("org.apache.kafka:kafka-streams:5.5.1-ccs")
  compileOnly("org.apache.kafka:kafka-clients:5.5.1-ccs")
  implementation("org.hypertrace.core.kafkastreams.framework:kafka-streams-serdes:0.1.11") {
    isTransitive = false
  }
}

dependencies {
  plugins(project(":${project.name}"))
}

tasks.register<Sync>("copyPlugins") {
  from(plugins)
  into("${buildDir}/plugins")
}

