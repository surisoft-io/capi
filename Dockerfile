FROM openjdk:17-jdk
ARG CAPI_VERSION=0

RUN mkdir /capi
RUN mkdir /capi/logs

ARG JAR_FILE=target/capi-lb-${CAPI_VERSION}.jar
COPY ${JAR_FILE} /capi/app.jar

ENTRYPOINT  exec java -XX:InitialHeapSize=2g \
                      -XX:MaxHeapSize=2g \
                      -XX:+HeapDumpOnOutOfMemoryError \
                      -XX:HeapDumpPath=/capi/logs/heap-dump.hprof \
                      --add-modules java.se \
                      --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
                      --add-opens java.base/java.lang=ALL-UNNAMED \
                      --add-opens java.base/java.nio=ALL-UNNAMED \
                      --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
                      --add-opens java.management/sun.management=ALL-UNNAMED \
                      --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
                      -jar /capi/app.jar
