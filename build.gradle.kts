plugins {
  id("org.hypertrace.repository-plugin") version "0.2.3"
  id("org.hypertrace.docker-plugin") version "0.7.1"
  id("org.hypertrace.docker-publish-plugin") version "0.7.1"
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot")
  }
}
