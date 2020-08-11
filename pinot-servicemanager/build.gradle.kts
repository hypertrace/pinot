plugins {
  id("org.hypertrace.docker-publish-plugin")
}

hypertraceDocker {
  defaultImage {
    imageName.set("pinot")
    tags.forEach { it.onlyIf { false } }
    tag("servicemanager")
  }
}
