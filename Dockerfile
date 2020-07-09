# pinot-confluent-avro plugin was missing from the distribution.
# https://github.com/apache/incubator-pinot/pull/5248

# Base image version is same as appVersion in helm/Chart.yaml file. Make sure to update it in both files.
FROM apachepinot/pinot:0.4.0-889889e20@sha256:5a1c436273c7196832d831eb2566a32e8f1dace13e2e2010ec42dfd873038d19 AS builder

FROM openjdk:8-jdk-slim

ENV PINOT_HOME=/opt/pinot

VOLUME ["${PINOT_HOME}/configs", "${PINOT_HOME}/data"]

COPY --from=builder ${PINOT_HOME} ${PINOT_HOME}

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

WORKDIR ${PINOT_HOME}

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
