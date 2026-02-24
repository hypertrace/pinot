# Choose libraries we need from Pinot's image.

# Download/install stage runs on the build host's native arch since Java JARs are arch-independent
FROM --platform=$BUILDPLATFORM ubuntu:jammy AS install

# Override to build an image from a fork. Ex. kotharironak
ARG JITPACK_USER=hypertrace
ARG JITPACK_TAG=hypertrace-1.0.0-1

WORKDIR /install

RUN apt-get update && apt-get install -y --no-install-recommends wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Copy hypertrace plugins
COPY build/plugins plugins/

COPY schemas/* schemas/

COPY install.sh /tmp/
RUN /tmp/install.sh && rm /tmp/install.sh

# Share the same base image to reduce layers used in testing
FROM eclipse-temurin:11-jre-jammy
LABEL MAINTAINER Hypertrace "https://www.hypertrace.org/"

# Add HEALTHCHECK and ENTRYPOINT scripts into the default search path
COPY docker-bin/* /usr/local/bin/

# All content including binaries and logs write under WORKDIR
ARG USER=pinot
WORKDIR /opt/pinot

# Ensure the process doesn't run as root
RUN adduser --disabled-password --gecos '' --home ${PWD} ${USER}
USER ${USER}

# Copy binaries and config we installed earlier
COPY --from=install --chown=${USER} /install .

# expose ports for controller/broker/server/admin/servicemanager
EXPOSE 9000 8099 8098 8097 8096 9514 7098

# We use start period of 45s to avoid marking Pinot unhealthy on slow or contended CI hosts
# * timeout is 10s instead of 5s as longer has happened in practice
HEALTHCHECK --interval=1s --start-period=45s --timeout=10s --retries=15 CMD ["docker-healthcheck"]

ENTRYPOINT ["start-servicemanager"]
