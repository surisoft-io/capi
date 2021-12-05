FROM openjdk:15-jdk 

RUN mkdir /capi
RUN mkdir /capi/logs

ARG JAR_FILE=capi-lb-0.0.1.jar
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
