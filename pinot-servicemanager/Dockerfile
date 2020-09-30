# Choose libraries we need from Pinot's image.

# Using the same image as we use in CircleCI to avoid transfer costs
FROM cimg/openjdk:14.0.2 AS install

# Override to build an image from a fork. Ex. kotharironak
ARG JITPACK_USER=apache
# Override to use a different expression, such as 'master-SNAPSHOT'
#   Note: 0.5.0 does not have parallel start (PR 5917): 21a372b2f58f9c6e27fe9710d6952ed519342061
ARG JITPACK_TAG=23797914c6fea24e34f3ebfca9a73e0fff72c2b7

USER root
WORKDIR /install

# Copy customer hypertrace plugins
COPY build/plugins plugins/

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
# * timeout is 10s instead of 5s as longer has happened in practice
HEALTHCHECK --interval=1s --start-period=45s --timeout=10s --retries=15 CMD ["docker-healthcheck"]

ENTRYPOINT ["start-servicemanager"]
