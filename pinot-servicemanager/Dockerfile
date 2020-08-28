# TODO: Update when pinot releases a SNAPSHOT or version at or past:
# 21a372b2f58f9c6e27fe9710d6952ed519342061 (parallel start)
FROM apachepinot/pinot:0.5.0-SNAPSHOT-331b874cd-20200821 as install

WORKDIR /install

COPY etc/* etc/

# Pinot starts
RUN mkdir classes && cd classes && \
  for JAR in \
  /opt/pinot/lib/* \
  /opt/pinot/plugins/pinot-input-format/pinot-confluent-avro/* \
  ; do jar -xf $JAR; done

# TODO: CircleCI is unable to extract the Kafka jar even if locally we can:
#   java.io.FileNotFoundException: kafka/coordinator/transaction/TransactionStateManager$$anonfun$enableTransactionalIdExpiration$1$$anonfun$apply$mcV$sp$1$$anonfun$kafka$coordinator$transaction$TransactionStateManager$$anonfun$$anonfun$$removeFromCacheCallback$1$1$$anonfun$apply$1$$anonfun$apply$mcV$sp$2.class (File name too long)
#	  at java.io.FileOutputStream.open0(Native Method)
# Leave this in a jar for now.
RUN mkdir libs && cp \
  /opt/pinot/plugins/pinot-stream-ingestion/pinot-kafka-2.0/* \
  libs;

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
