plugins {
  `java-library`
}

// Pinot images run with Java 11, not JDK 14
//
// If any other dependencies are added, make sure they also are limited accordingly
configure<JavaPluginExtension> {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
  implementation("org.hypertrace.core.attribute.service:attribute-projection-functions:0.3.4-SNAPSHOT")
  compileOnly("org.apache.pinot:pinot-common:0.5.0")
}
