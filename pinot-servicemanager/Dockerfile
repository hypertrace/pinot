# TODO: update SNAPSHOT to get parallel branch content
FROM apachepinot/pinot:0.5.0-SNAPSHOT-331b874cd-20200821 as install

WORKDIR /install

COPY etc/* etc/

# Cherry-pick jars we need from Pinot from their image and extract them
# to speed startup
RUN mkdir classes && cd classes && \
  for JAR in \
  /opt/pinot/lib/* \
  /opt/pinot/plugins/pinot-input-format/pinot-confluent-avro/* \
  /opt/pinot/plugins/pinot-stream-ingestion/pinot-kafka-2.0/* \
  ; do jar -xf $JAR; done

# Share the same base image to reduce layers used in testing
FROM hypertrace/java:11
MAINTAINER Hypertrace "https://www.hypertrace.org/"

# Add HEALTHCHECK and ENTRYPOINT scripts into the default search path
COPY docker-bin/* /usr/local/bin/

ENV PINOT_HOME=/opt/pinot
ENV PINOT_VAR=/var/pinot
ENV JAVA_OPTS="-Xms256M -Xmx512M -XX:MaxDirectMemorySize=96M -XX:+ExitOnOutOfMemoryError"

WORKDIR ${PINOT_HOME}

# Ensure the process doesn't run as root
ARG USER=pinot
RUN adduser -g '' -h ${PWD} -D ${USER} && \
  mkdir -p ${PINOT_VAR}/server/data ${PINOT_VAR}/controller/data && \
  chown -R ${USER} ${PINOT_VAR}
USER ${USER}

# Copy binaries and config we installed earlier
COPY --from=install --chown=${USER} /install .

# expose ports for controller/broker/server/admin/servicemanager
EXPOSE 9000 8099 8098 8097 8096 9514 7098

# We use start period of 45s to avoid marking Pinot unhealthy on slow or contended CI hosts
HEALTHCHECK --interval=1s --start-period=45s --timeout=5s --retries=15 CMD ["docker-healthcheck"]

ENTRYPOINT ["start-servicemanager"]
