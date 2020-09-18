#!/bin/sh
# install script used only in building the docker image, but not at runtime.
# This uses relative path so that you can change the home dir without editing this file.
# This also trims dependencies to only those used at runtime.
set -eux

mkdir etc

cat > etc/log4j2.properties <<-'EOF'
appenders=console
appender.console.type=Console
appender.console.name=STDOUT
appender.console.layout.type=PatternLayout
appender.console.layout.pattern=%d{ABSOLUTE} %-5p [%t] %C{2} (%F:%L) - %m%n
rootLogger.level=warn
rootLogger.appenderRefs=stdout
rootLogger.appenderRef.stdout.ref=STDOUT
# Hush reflections similarly to https://github.com/apache/incubator-pinot/pull/5001
logger.reflections.name=org.reflections
logger.reflections.level=fatal
# Hush Helix warnings until https://github.com/apache/incubator-pinot/issues/5974
logger.helix.name=org.apache.helix
logger.helix.level=fatal
# Ensure we can see timing messages
logger.servicemanager.name=org.apache.pinot.tools.admin.command.StartServiceManagerCommand
logger.servicemanager.level=info
EOF

cat > etc/pinot-broker.conf <<-'EOF'
pinot.service.role=BROKER
pinot.set.instance.id.to.hostname=true
pinot.broker.routing.table.builder.class=random
pinot.broker.timeoutMs=60000
EOF

# We set the following at startup:
# * controller.data.dir
# * controller.zk.str
cat > etc/pinot-controller.conf <<-'EOF'
pinot.service.role=CONTROLLER
pinot.set.instance.id.to.hostname=true
# controller requires explicit port
controller.port=9000
controller.helix.cluster.name=hypertrace-views
EOF

# We set the following at startup:
# * pinot.server.instance.dataDir
# * pinot.server.instance.segmentTarDir
cat > etc/pinot-server.conf <<-'EOF'
pinot.service.role=SERVER
pinot.set.instance.id.to.hostname=true
pinot.server.instance.realtime.alloc.offheap=true
pinot.server.query.executor.timeout=60000
EOF

echo "*** Image build complete"