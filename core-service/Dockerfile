FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Копируем модуль целиком, чтобы Gradle Wrapper мог собрать bootJar внутри контейнера
COPY core-service/ ./

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=builder /app/build/libs/core-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
