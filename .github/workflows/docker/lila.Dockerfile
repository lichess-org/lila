FROM eclipse-temurin:21-jre

WORKDIR /lila

COPY lila/ .
COPY conf/application.conf.default conf/application.conf

RUN chmod +x bin/lila

CMD ["bin/lila", "-Dconfig.file=conf/application.conf"]
