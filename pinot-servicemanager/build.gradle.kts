plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    tasks.named(buildTaskName) {
      dependsOn("copyPlugins")
    }
  }
}

val plugins by configurations.creating

dependencies {
  plugins(project(":pinot-udf"))
  plugins(project(":pinot-minion-tasks"))
  plugins(project(":pinot-avro-serde"))
}

tasks.register<Sync>("copyPlugins") {
  from(plugins)
  into("${buildDir}/plugins")
}
