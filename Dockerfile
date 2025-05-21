FROM eclipse-temurin:21-jdk-alpine

ARG CAPI_VERSION=4.8.20

RUN mkdir /capi
RUN mkdir /capi/logs

ARG JAR_FILE=target/capi-${CAPI_VERSION}.jar
COPY ${JAR_FILE} /capi/app.jar

ENTRYPOINT exec java -XX:InitialHeapSize=512m \
                     -XX:+UseG1GC \
                     -XX:MaxGCPauseMillis=100 \
                     -XX:+ParallelRefProcEnabled \
                     -XX:+HeapDumpOnOutOfMemoryError \
                     -XX:HeapDumpPath=/capi/logs/heap-dump.hprof \
                     -jar /capi/app.jar
