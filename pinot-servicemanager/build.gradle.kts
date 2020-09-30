plugins {
  `java-library`
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot-servicemanager")
    //dockerFile.set(file("Dockerfile"))
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
  implementation("org.hypertrace.core.kafkastreams.framework:kafka-streams-serdes:0.1.11")
}

dependencies {
  plugins(project(":${project.name}"))
}

tasks.register<Sync>("copyPlugins") {
  from(plugins).include("pinot-servicemanager*").include("kafka-streams-serdes*")
  into("${buildDir}/plugins")
}

