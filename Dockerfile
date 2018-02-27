FROM openjdk:8
LABEL maintainer="biomodels-developers@lists.sf.net"

# install sdkman and Grails
RUN apt-get update && apt-get install -y --no-install-recommends zip
RUN curl -s "https://get.sdkman.io" | bash
RUN ["/bin/bash", "-lc", "source $HOME/.sdkman/bin/sdkman-init.sh"]
RUN ["/bin/bash", "-lc", "sdk install grails 2.5.5"]

# expected database port
EXPOSE 3306

COPY . /usr/src/jummp-biomodels
WORKDIR /usr/src/jummp-biomodels

ENTRYPOINT ["/bin/bash", "-lc", "grails"]
