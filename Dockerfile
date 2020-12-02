FROM alpine:latest AS builder

ENV PINOT_VERSION=0.6.0

RUN wget -qO- https://downloads.apache.org/incubator/pinot/apache-pinot-incubating-$PINOT_VERSION/apache-pinot-incubating-$PINOT_VERSION-bin.tar.gz | tar -xzf- && \
    mv apache-pinot-incubating-$PINOT_VERSION-bin /pinot && \
    rm -rf /pinot/examples

FROM openjdk:11-jdk-slim
LABEL maintainer="Hypertrace https://www.hypertrace.org/"

ENV PINOT_HOME=/opt/pinot

VOLUME ["${PINOT_HOME}/configs", "${PINOT_HOME}/data"]

COPY --from=builder /pinot ${PINOT_HOME}
COPY build/plugins "${PINOT_HOME}/plugins"

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

WORKDIR ${PINOT_HOME}

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
