FROM biomodels/jummp-biomodels:1.2-dependencies
LABEL maintainer="biomodels-developers@lists.sf.net"

# install tools and utilities
RUN apt-get update && apt-get install -y mysql-client && rm -rf /var/lib/apt

# set environment options
#ENV MAVEN_OPTS="-Xmx1024m"
ENV JAVA_OPTS="-Xms64m -Xmx1024m -XX:MaxMetaspaceSize=128m"
ENV GRAILS_OPTS="-server -Xmx2g -Xms2g -Dfile.encoding=UTF-8"
#ENV ANT_OPTS="-Xmx2g -XX:MaxPermSize=1024m"
# expected database port
EXPOSE 3306
EXPOSE 4372
EXPOSE 8080

# Add an app user so our program doesn't run as root.
### this is ur virtual users: $USER_NAME
ARG USER_NAME_ENV
ARG USER_ID_ENV
ARG GROUP_NAME_ENV
ARG GROUP_ID_ENV

ENV UID=$USER_ID_ENV
ENV USERNAME=$USER_NAME_ENV
### this is virtual group: pst_pub
ENV GID=$GROUP_ID_ENV
ENV GROUP=GROUP_NAME_ENV
ENV HOME=/home/$USERNAME
RUN mkdir -p $HOME
RUN addgroup --gid "$GID" "$USERNAME" \
   && adduser \
   --uid "$UID" \
   --disabled-password \
   --gecos "" \
   --ingroup "$USERNAME" \
   --home $HOME \
   "$USERNAME"

ENV APP_HOME=/home/$USERNAME/biomodels

## SETTING UP THE APP ##
RUN mkdir -p $APP_HOME
RUN chown -R $USERNAME:$USERNAME $HOME

# ***
# Do any custom logic needed prior to adding your code here
# ***

# Copy in the application code.
#ADD . $APP_HOME
# Chown all the files to the app user.
#RUN chown -R "$USERNAME":"$USERNAME" $APP_HOME

# Below is equivalent to the above commands, just speeds up and reduces image size
COPY --chown=$USERNAME:$USERNAME . $APP_HOME

# Change to the app user.
USER $USERNAME
WORKDIR $APP_HOME
RUN ["chmod", "+x", "grailsw"]
ENTRYPOINT ["/bin/bash", "-lc", "./grailsw prod run-app --non-interactive --stacktrace"]
