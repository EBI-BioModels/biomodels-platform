##
# Docker build file for BioModels.
#
# Defines three stages
#   * base: for general purpose development/testing
#   * prod: for running in production
#   * debug: for debugging over JPDA
#
# See https://bitbucket.org/biomodels/jummp-biomodels/src/master/docker-build.sh
##
FROM openjdk:8-jdk AS base
LABEL maintainer="biomodels-developers@lists.sf.net"

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

# the docker image to be used in production
FROM tomcat:7-jdk8-openjdk-slim AS prod
LABEL maintainer="biomodels-developers@lists.sf.net"

# set environment options
ENV JAVA_OPTS="-Xms64m -Xmx1024m -XX:MaxMetaspaceSize=128m -server -noverify -XX:+UseConcMarkSweepGC -XX:+UseParNewGC"

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
COPY ./target/jummp-biomodels.war /usr/local/tomcat/webapps/ROOT.war
RUN mkdir webapps/ROOT; \
    cd webapps/ROOT; \
    jar xf ../ROOT.war; \
    cd - ; \
    mkdir log data; \
    chown -R $USERNAME /usr/local/tomcat/data /usr/local/tomcat/log;

# Change to the app user.
USER $USERNAME

CMD ["catalina.sh", "run"]


# the docker image for troubleshooting/debugging
FROM prod as debug
ARG JPDA_PORT=8089

EXPOSE $JPDA_PORT

# use address=*:$JPDA_PORT on java 9+
ENV JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$JPDA_PORT"
ENV JAVA_OPTS="$JAVA_OPTS $JAVA_DEBUG_OPTS"

CMD ["catalina.sh", "run"]
