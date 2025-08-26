FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar


ENTRYPOINT exec java -XX:InitialHeapSize=512m \
                     -XX:+UseG1GC \
                     -XX:MaxGCPauseMillis=100 \
                     -XX:+ParallelRefProcEnabled \
                     -XX:+HeapDumpOnOutOfMemoryError \
                     -XX:HeapDumpPath=/capi/logs/heap-dump.hprof \
                     -jar app.jar