##
# Docker build file for BioModels.
#
# Defines three stages
#   * base: for general purpose development/testing
#   * prod: for running in production
#   * develop: for running in development
#   * debug: for debugging over JPDA
#
# See https://bitbucket.org/biomodels/jummp-biomodels/src/master/docker-build.sh
##
FROM openjdk:8-jdk-buster AS base
LABEL maintainer="biomodels-developers@lists.sf.net"

# install some utilities
RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    net-tools vim telnet \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

# install Java and Grails based on
# https://github.com/mozart-analytics/grails-docker/blob/c65d488/grails-2/Dockerfile
ENV GRAILS_VERSION 2.5.5

# Install Grails
WORKDIR /usr/lib/jvm
RUN wget https://github.com/grails/grails-core/releases/download/v$GRAILS_VERSION/grails-$GRAILS_VERSION.zip && \
    unzip grails-$GRAILS_VERSION.zip && \
    rm -rf grails-$GRAILS_VERSION.zip && \
    ln -s grails-$GRAILS_VERSION grails

# Setup Grails path.
ENV GRAILS_HOME /usr/lib/jvm/grails
ENV PATH $GRAILS_HOME/bin:$PATH
# map $PWD to /app when running this image
# don't forget to also mount the folder containing the runtime configuration file
WORKDIR /app/

# the docker image to be used in the following stages
FROM tomcat:7-jdk8-openjdk-buster AS deploy
LABEL maintainer="biomodels-developers@lists.sf.net"

# set environment options
# see https://stackoverflow.com/a/59097932 for more information about potentially needing to use -Djava.security.egd=file:/dev/./urandom in JDK9+
ENV JAVA_OPTS_1="-Xms3g -Xmx3g -XX:MaxPermSize=512m -XX:MaxMetaspaceSize=512m"
ENV JAVA_OPTS_2="-XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:MaxJavaStackTraceDepth=100"
ENV JAVA_OPTS_3="-XX:+HeapDumpOnOutOfMemoryError"
ENV JAVA_OPTS="$JAVA_OPTS_1 $JAVA_OPTS_2 $JAVA_OPTS_3 -server -noverify -Djava.net.preferIPv4Stack=true"
EXPOSE 3306
EXPOSE 4372
EXPOSE 6379
EXPOSE 8080

ARG UID
ARG USERNAME
ARG GID
ARG GROUP

ENV HOME=/home/$USERNAME
RUN mkdir -p $HOME
RUN addgroup --gid "$GID" "$USERNAME" \
   && adduser \
   --uid "$UID" \
   --disabled-password \
   --gecos "" \
   --ingroup "$USERNAME" \
   --no-create-home \
   "$USERNAME"; \
   chown -R $USERNAME:$USERNAME $HOME

# Tomcat manager and host-manager can be copied from webapps.dist if needed
RUN rm -rf /usr/local/tomcat/webapps/*
COPY ./target/biomodels.war /usr/local/tomcat/webapps/biomodels.war
RUN mkdir webapps/biomodels; \
    cd webapps/biomodels; \
    jar xf ../biomodels.war; \
    cd - ; \
    mkdir log data; \
    chown -R $USERNAME /usr/local/tomcat/data /usr/local/tomcat/log /usr/local/tomcat/webapps;

# the docker image for the production server
FROM deploy as prod
COPY ./k8s/init/start.sh /usr/local/tomcat/bin/
USER $USERNAME
CMD ["catalina.sh", "run"]

# the docker image for the development server
FROM deploy as develop
USER $USERNAME
CMD ["catalina.sh", "run"]

# the docker image for troubleshooting/debugging
FROM deploy as debug
ARG JPDA_PORT=8089

EXPOSE $JPDA_PORT

# use address=*:$JPDA_PORT on java 9+
ENV JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$JPDA_PORT"
ENV JAVA_OPTS="$JAVA_OPTS $JAVA_DEBUG_OPTS"

CMD ["catalina.sh", "run"]
