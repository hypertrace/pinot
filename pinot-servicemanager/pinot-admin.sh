#!/bin/sh
# For java 9 and later version, we need to explicitly set Pinot Plugins directory into classpath.
CLASSPATH=$(find lib plugins -type f | xargs echo | tr ' ' ':')
# TODO: link to pinot issue why we need internal access
exec java \
  --add-opens java.base/jdk.internal.ref=ALL-UNNAMED \
  -classpath $CLASSPATH \
  $JAVA_OPTS \
  -Dlog4j.configurationFile=etc/log4j2.properties \
  -Dapp.name=pinot-admin \
  -Dapp.pid=$$ \
  -Dapp.repo=$PINOT_HOME/lib \
  -Dapp.home=$PINOT_HOME \
  -Dbasedir=$PINOT_HOME \
  org.apache.pinot.tools.admin.PinotAdministrator \
  "$@"
