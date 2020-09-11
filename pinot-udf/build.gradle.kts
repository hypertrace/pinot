plugins {
  `java-library`
}

dependencies {
  implementation("org.hypertrace.core.attribute.service:attribute-projection-functions:0.3.4-SNAPSHOT")
  compileOnly("org.apache.pinot:pinot-common:0.5.0")
}
