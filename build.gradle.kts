plugins {
  id("org.hypertrace.repository-plugin") version "0.4.0"
  id("org.hypertrace.docker-plugin") version "0.9.4"
  id("org.hypertrace.docker-publish-plugin") version "0.9.4"
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
}

tasks.register<Sync>("copyPlugins") {
  from(plugins)
  into("${buildDir}/plugins")
}

subprojects {
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }
}
