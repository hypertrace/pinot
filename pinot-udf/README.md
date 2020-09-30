# Pinot UDFs

## Requirements
These UDFs should have a minimal dependency tree and be compiled to support a java target no
higher than our pinot images use as a runtime. Currently, that means java 11. Any common dependencies
in the dependency tree (which can be seen via `gradlew dependencies --configuration runtimeClasspath`)
must be shaded, so should be avoided.

## Warning on versions
As of [2379791](https://github.com/apache/incubator-pinot/commit/23797914c6fea24e34f3ebfca9a73e0fff72c2b7)
(between 0.5.0 and 0.6.0) pinot introduces a breaking change moving the ScalarFunction annotation to
a new packacge. Because these UDFs are used by both the main pinot image and the
pinot-servicemanager image, both images must use either an earlier version or a later version
(although they should be in sync regardless), and the compile dependency on pinot in this module
should reflect that same version.

## Rationale
This UDF jar is added as a plugin to the pinot images built in this repository. The UDFs are used to
support projected attributes, where we support creating new attributes as read time-only projections,
using a combination of custom UDFs and those built in to pinot.