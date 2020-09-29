pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://dl.bintray.com/hypertrace/maven")
  }
}
rootProject.name = "pinot"
plugins {
  id("org.hypertrace.version-settings") version "0.1.1"
}

include(":pinot-servicemanager")
