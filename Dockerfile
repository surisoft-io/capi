FROM openjdk:17-jdk
ARG CAPI_VERSION=3.0.20

RUN mkdir /capi
RUN mkdir /capi/logs

ARG JAR_FILE=target/capi-lb-${CAPI_VERSION}.jar
COPY ${JAR_FILE} /capi/app.jar

ENTRYPOINT  exec java -XX:InitialHeapSize=2g \
                      -XX:MaxHeapSize=2g \
                      -XX:+HeapDumpOnOutOfMemoryError \
                      -XX:HeapDumpPath=/capi/logs/heap-dump.hprof \
                      -jar /capi/app.jar
