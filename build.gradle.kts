plugins {
  id("org.hypertrace.docker-plugin") version "0.7.1"
  id("org.hypertrace.docker-publish-plugin") version "0.7.1"
  id("org.hypertrace.docker-java-application-plugin") version "0.7.1" apply false
  id("org.hypertrace.repository-plugin") version "0.2.3"
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot")
  }
}
