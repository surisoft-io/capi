FROM eclipse-temurin:21-jdk-alpine

ARG CAPI_VERSION=4.8.20

RUN mkdir /capi
RUN mkdir /capi/logs

ARG JAR_FILE=target/capi-${CAPI_VERSION}.jar
COPY ${JAR_FILE} /capi/app.jar

ENTRYPOINT exec java -XX:InitialHeapSize=512m \
                     -XX:MaxHeapSize=1g \
                     -XX:+UseG1GC \
                     -XX:+HeapDumpOnOutOfMemoryError \
                     -XX:HeapDumpPath=/capi/logs/heap-dump.hprof \
                     -jar /capi/app.jar
