FROM amd64/ubuntu:jammy AS builder

ARG PINOT_VERSION=1.0.0
ARG JITPACK_REPO=hypertrace/incubator-pinot
ARG JITPACK_TAG=hypertrace-1.0.0-rc2

ENV PINOT_HOME=/opt/pinot

RUN apt-get -y update && apt-get -y install curl libjemalloc-dev

# Create directory structure
RUN curl -L https://archive.apache.org/dist/pinot/apache-pinot-$PINOT_VERSION/apache-pinot-$PINOT_VERSION-bin.tar.gz | tar -xzf- && \
    mv apache-pinot-$PINOT_VERSION-bin $PINOT_HOME && \
    rm -rf $PINOT_HOME/examples && \
    rm -rf $PINOT_HOME/lib/* && \
    rm -rf $PINOT_HOME/plugins/* && \
    rm -rf $PINOT_HOME/plugins-external/*

# Fetch jar
RUN curl -L -o $PINOT_HOME/lib/pinot-all-${JITPACK_TAG}-shaded.jar \
        https://jitpack.io/com/github/${JITPACK_REPO}/pinot-distribution/${JITPACK_TAG}/pinot-distribution-${JITPACK_TAG}-shaded.jar

# Fetch plugin jars
RUN for artifactId in pinot-kafka-2.0 pinot-kinesis pinot-thrift pinot-json pinot-csv pinot-confluent-avro pinot-avro pinot-protobuf pinot-batch-ingestion-standalone pinot-batch-ingestion-hadoop pinot-hdfs pinot-gcs pinot-s3 pinot-dropwizard; do \
      curl -L -o $PINOT_HOME/plugins/${artifactId}-${JITPACK_TAG}-shaded.jar \
          https://jitpack.io/com/github/${JITPACK_REPO}/${artifactId}/${JITPACK_TAG}/${artifactId}-${JITPACK_TAG}-shaded.jar; \
    done; \
    for artifactId in pinot-minion-builtin-tasks pinot-segment-uploader-default pinot-segment-writer-file-based; do \
      curl -L -o $PINOT_HOME/plugins/${artifactId}-${JITPACK_TAG}.jar \
          https://jitpack.io/com/github/${JITPACK_REPO}/${artifactId}/${JITPACK_TAG}/${artifactId}-${JITPACK_TAG}.jar; \
    done

FROM amd64/eclipse-temurin:11-jre-jammy
LABEL maintainer="Hypertrace https://www.hypertrace.org/"

ENV PINOT_HOME=/opt/pinot
RUN apt update && apt upgrade -y && rm -rf /var/lib/apt/lists/*

VOLUME ["${PINOT_HOME}/configs", "${PINOT_HOME}/data"]

COPY --from=builder ${PINOT_HOME} ${PINOT_HOME}
COPY build/plugins "${PINOT_HOME}/plugins"

# use jemalloc
COPY --from=builder /usr/lib/x86_64-linux-gnu/libjemalloc* /usr/lib/x86_64-linux-gnu/
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

WORKDIR ${PINOT_HOME}

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
