plugins {
  `java-library`
}

dependencies {
  implementation("org.hypertrace.core.attribute.service:attribute-projection-functions:0.4.1")
  compileOnly("org.apache.pinot:pinot-common:0.5.0")
}
