rootProject.name = "pinot"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://dl.bintray.com/hypertrace/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.1.1"
}

include(":pinot-servicemanager")
include(":pinot-udf")
include(":pinot-avro-serde")
include(":pinot-minion-tasks")
