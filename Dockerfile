# TODO: Move to stable release containing the following fixes
# Temporarily using snapshot build including the following fixes.
# https://github.com/apache/incubator-pinot/pull/5248
# https://github.com/apache/incubator-pinot/pull/5890

# Base image version is same as appVersion in helm/Chart.yaml file. Make sure to update it in both files.
FROM laxmanch/pinot:0.4.0-89cc0e113@sha256:f769db0a6095568f90b52f16b7cf8354a6533acf62e67459ea0d0c6444fd4b27 AS builder

FROM openjdk:8-jdk-slim

ENV PINOT_HOME=/opt/pinot

VOLUME ["${PINOT_HOME}/configs", "${PINOT_HOME}/data"]

COPY --from=builder ${PINOT_HOME} ${PINOT_HOME}

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

WORKDIR ${PINOT_HOME}

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
