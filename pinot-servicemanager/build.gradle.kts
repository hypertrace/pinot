plugins {
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
val plugins by configurations.creating

dependencies {
  plugins(project(":pinot-udf"))
}

tasks.register<Sync>("copyPlugins") {
  from(plugins)
  into("${buildDir}/plugins")
}
