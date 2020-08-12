FROM apachepinot/pinot:0.5.0-SNAPSHOT-31d4194a2-20200731 as pinot

WORKDIR /pinot
COPY pinot-admin.sh bin/pinot-admin.sh
COPY etc/* etc/

# Cherry-pick files we need from Pinot from their image
RUN cd /opt/pinot && \
  tar -cpf - lib/* \
  plugins/pinot-input-format/pinot-confluent-avro \
  plugins/pinot-stream-ingestion/pinot-kafka-2.0 | \
  (cd /pinot; tar -xpf -)

# Prepage changes over base Java image
FROM hypertrace/java:11
LABEL MAINTAINER Hypertrace "https://www.hypertrace.org/"

ENV PINOT_HOME=/opt/pinot
ENV PINOT_VAR=/var/pinot
ENV JAVA_OPTS="-Xms128M -Xmx192M -XX:MaxDirectMemorySize=64M -XX:+ExitOnOutOfMemoryError"

WORKDIR ${PINOT_HOME}

ARG USER=pinot

# Setup pinot user so that the image doesn't run as root
RUN adduser -g '' -h ${PINOT_HOME} -D ${USER} && \
    mkdir -p ${PINOT_VAR}/server/data ${PINOT_VAR}/controller/data && \
    chown -R ${USER} ${PINOT_VAR}

USER ${USER}

COPY --from=pinot --chown=${USER} /pinot ${PINOT_HOME}

# expose ports for controller/broker/server/admin
EXPOSE 9000 8099 8098 8097 8096 9514

# Healthcheck is against the controller as that's what clients connect to
HEALTHCHECK --interval=1s --start-period=30s --timeout=1s CMD wget -qO- http://127.0.0.1:9000/pinot-controller/admin &> /dev/null || exit 1

ENTRYPOINT ["./bin/pinot-admin.sh"]

CMD ["run"]
