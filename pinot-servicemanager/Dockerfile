# Choose libraries we need from Pinot's image.
FROM apachepinot/pinot:0.5.0-SNAPSHOT-e0ed179c2-20200910 as install

WORKDIR /install

# TODO: use maven to do get deps so that we can avoid shaded jars
RUN mkdir libs && cp \
  /opt/pinot/lib/* \
  /opt/pinot/plugins/pinot-input-format/pinot-confluent-avro/* \
  /opt/pinot/plugins/pinot-stream-ingestion/pinot-kafka-2.0/* \
  libs;

# Pinot starts faster when classes are extracted
# TODO: When we switch to Maven for pinot, use maven-dependency-plugin:unpack
RUN mkdir classes && cd classes && \
  (for JAR in ../libs/*; do jar -xf $JAR; done) && \
  cd .. && rm -rf libs

COPY schemas/* schemas/

COPY install.sh /tmp/
RUN /tmp/install.sh && rm /tmp/install.sh

# Share the same base image to reduce layers used in testing
FROM hypertrace/java:11
LABEL MAINTAINER Hypertrace "https://www.hypertrace.org/"

# Add HEALTHCHECK and ENTRYPOINT scripts into the default search path
COPY docker-bin/* /usr/local/bin/

# All content including binaries and logs write under WORKDIR
ARG USER=pinot
WORKDIR /opt/pinot

# Ensure the process doesn't run as root
RUN adduser -g '' -h ${PWD} -D ${USER}
USER ${USER}

# Copy binaries and config we installed earlier
COPY --from=install --chown=${USER} /install .

# expose ports for controller/broker/server/admin/servicemanager
EXPOSE 9000 8099 8098 8097 8096 9514 7098

# We use start period of 45s to avoid marking Pinot unhealthy on slow or contended CI hosts
HEALTHCHECK --interval=1s --start-period=45s --timeout=5s --retries=15 CMD ["docker-healthcheck"]

ENTRYPOINT ["start-servicemanager"]
