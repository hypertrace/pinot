rootProject.name = "pinot"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://hypertrace.jfrog.io/artifactory/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.2.0"
}

include(":pinot-servicemanager")
include(":pinot-udf")
include(":pinot-avro-serde")
include(":pinot-minion-tasks")
