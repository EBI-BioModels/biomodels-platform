FROM openjdk:8
LABEL maintainer="biomodels-developers@lists.sf.net"


RUN mkdir -p /home/biomodels/jummp-biomodels
COPY . /home/biomodels/jummp-biomodels
WORKDIR /home/biomodels/jummp-biomodels

# expected database port
EXPOSE 3306

RUN chmod +x /home/biomodels/jummp-biomodels/grailsw
ENTRYPOINT /home/biomodels/jummp-biomodels/grailsw run-war
