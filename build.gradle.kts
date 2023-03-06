plugins {
  id("org.hypertrace.repository-plugin") version "0.4.0"
  id("org.hypertrace.docker-plugin") version "0.9.9"
  id("org.hypertrace.docker-publish-plugin") version "0.9.9"
  id("org.owasp.dependencycheck") version "8.1.2"
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

dependencyCheck {
  format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
  suppressionFile = "owasp-suppressions.xml"
  scanConfigurations.add("runtimeClasspath")
  failBuildOnCVSS = 7.0F
}
