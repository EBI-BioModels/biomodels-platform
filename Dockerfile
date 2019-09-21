FROM openjdk:8
LABEL maintainer="biomodels-developers@lists.sf.net"

# set environment options
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m"
ENV GRAILS_OPTS="-server -Xmx768M -Xms64M -XX:PermSize=32m -XX:MaxPermSize=256m -Dfile.encoding=UTF-8"

RUN mkdir -p /home/biomodels/jummp-biomodels
COPY . /home/biomodels/jummp-biomodels
WORKDIR /home/biomodels/jummp-biomodels

# expected database port
EXPOSE 3306

RUN chmod +x /home/biomodels/jummp-biomodels/grailsw
ENTRYPOINT /home/biomodels/jummp-biomodels/grailsw run-war
