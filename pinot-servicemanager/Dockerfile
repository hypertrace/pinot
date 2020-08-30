# Choose libraries we need from Pinot's image.
FROM apachepinot/pinot:0.5.0-SNAPSHOT-a892fb40b-20200829 as pinot

WORKDIR /pinot

# TODO: use maven to do get deps so that we can avoid shaded jars
RUN mkdir libs && cp \
  /opt/pinot/lib/* \
  /opt/pinot/plugins/pinot-input-format/pinot-confluent-avro/* \
  /opt/pinot/plugins/pinot-stream-ingestion/pinot-kafka-2.0/* \
  libs;

# Using the same image as we use in CircleCI to avoid transfer costs
# and avoid nuance in Pinot image until we stop using it for normal Maven
FROM cimg/openjdk:14.0.2 AS install

WORKDIR /install
USER root

COPY etc/* etc/

COPY --from=pinot /pinot .

# Pinot starts faster when classes are extracted
# TODO: When we switch to Maven for pinot, use maven-dependency-plugin:unpack
RUN mkdir classes && \
  (for JAR in libs/*; do unzip -qo $JAR -d classes; done) && \
  rm -rf libs

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
