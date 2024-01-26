FROM openjdk:21-jdk

RUN mkdir /capi
RUN mkdir /capi/logs

ARG JAR_FILE=target/capi-${{ env.RELEASE_VERSION }}.jar
COPY ${JAR_FILE} /capi/app.jar

ENTRYPOINT  exec java -XX:InitialHeapSize=4g \
                      -XX:MaxHeapSize=4g \
                      -XX:+HeapDumpOnOutOfMemoryError \
                      -XX:HeapDumpPath=/capi/logs/heap-dump.hprof \
                      -jar /capi/app.jar
