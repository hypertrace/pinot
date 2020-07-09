plugins {
  id("org.hypertrace.docker-plugin") version "0.2.2"
  id("org.hypertrace.docker-publish-plugin") version "0.2.2"
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot")
  }
}
