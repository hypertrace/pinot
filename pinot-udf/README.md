# Pinot UDFs

## Requirements
These UDFs should have a minimal dependency tree and be compiled to support a java target no
higher than our pinot images use as a runtime. Currently, that means java 11. Any common dependencies
in the dependency tree (which can be seen via `gradlew dependencies --configuration runtimeClasspath`)
must be shaded, so should be avoided.

## Rationale
This UDF jar is added as a plugin to the pinot images built in this repository. The UDFs are used to
support projected attributes, where we support creating new attributes as read time-only projections,
using a combination of custom UDFs and those built in to pinot.