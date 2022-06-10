FROM alpine:latest AS builder

ARG PINOT_VERSION=0.10.0
ARG JITPACK_REPO=hypertrace/incubator-pinot
ARG JITPACK_TAG=hypertrace-0.10.0-2

ENV PINOT_HOME=/opt/pinot

RUN apk add --update curl

# Create directory structure
RUN curl -L https://archive.apache.org/dist/pinot/apache-pinot-$PINOT_VERSION/apache-pinot-$PINOT_VERSION-bin.tar.gz | tar -xzf- && \
    mv apache-pinot-$PINOT_VERSION-bin $PINOT_HOME && \
    rm -rf $PINOT_HOME/examples && \
    rm -rf $PINOT_HOME/lib/* && \
    rm -rf $PINOT_HOME/plugins/*

# Fetch jar
RUN curl -L -o $PINOT_HOME/lib/pinot-all-${JITPACK_TAG}-shaded.jar \
        https://jitpack.io/com/github/${JITPACK_REPO}/pinot-distribution/${JITPACK_TAG}/pinot-distribution-${JITPACK_TAG}-shaded.jar

# Fetch plugin jars
RUN for artifactId in pinot-kafka-2.0 pinot-kinesis pinot-thrift pinot-json pinot-parquet pinot-orc pinot-csv pinot-confluent-avro pinot-avro pinot-protobuf pinot-batch-ingestion-standalone pinot-batch-ingestion-hadoop pinot-batch-ingestion-spark pinot-hdfs pinot-adls pinot-gcs pinot-s3 pinot-minion-builtin-tasks pinot-segment-uploader-default pinot-segment-writer-file-based pinot-dropwizard pinot-yammer; do \
      curl -L -o $PINOT_HOME/plugins/${artifactId}-${JITPACK_TAG}-shaded.jar \
          https://jitpack.io/com/github/${JITPACK_REPO}/${artifactId}/${JITPACK_TAG}/${artifactId}-${JITPACK_TAG}-shaded.jar; \
    done

FROM openjdk:11-jre-slim
LABEL maintainer="Hypertrace https://www.hypertrace.org/"

ENV PINOT_HOME=/opt/pinot

VOLUME ["${PINOT_HOME}/configs", "${PINOT_HOME}/data"]

COPY --from=builder ${PINOT_HOME} ${PINOT_HOME}
COPY build/plugins "${PINOT_HOME}/plugins"

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

WORKDIR ${PINOT_HOME}

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
