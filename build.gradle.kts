plugins {
  id("org.hypertrace.docker-plugin") version "0.7.1"
  id("org.hypertrace.docker-publish-plugin") version "0.7.1"
  id("org.hypertrace.repository-plugin") version "0.2.3"
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
}

tasks.register<Sync>("copyPlugins") {
  from(plugins)
  into("${buildDir}/plugins")
}
