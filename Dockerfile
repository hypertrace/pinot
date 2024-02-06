FROM amd64/ubuntu:jammy AS builder

ARG PINOT_VERSION=0.12.0
ARG JITPACK_REPO=hypertrace/incubator-pinot
ARG JITPACK_TAG=hypertrace-0.12.0-11

ENV PINOT_HOME=/opt/pinot

RUN apt-get -y update && apt-get -y install curl

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

FROM amd64/eclipse-temurin:11-jdk-jammy
LABEL maintainer="Hypertrace https://www.hypertrace.org/"

ENV PINOT_HOME=/opt/pinot
RUN apt update && apt upgrade -y && apt install curl libunwind-dev -y && rm -rf /var/lib/apt/lists/*

VOLUME ["${PINOT_HOME}/configs", "${PINOT_HOME}/data"]

COPY --from=builder ${PINOT_HOME} ${PINOT_HOME}
COPY build/plugins "${PINOT_HOME}/plugins"

# async profiler for debugging
RUN cd /opt && curl -L -o async-profiler.tar.gz https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz && tar -xzf async-profiler.tar.gz && rm async-profiler.tar.gz

# jemalloc custom install
RUN cd /opt && curl -L -o jemalloc.tar.gz https://github.com/jemalloc/jemalloc/archive/refs/tags/5.3.0.tar.gz && tar -xzf jemalloc.tar.gz && rm jemalloc.tar.gz && cd jemalloc-5.3.0 && ./configure --prefix=/home/kishan/install/jemalloc --enable-prof --enable-prof-libunwind --enable-stats && make && make install
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

WORKDIR ${PINOT_HOME}

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
