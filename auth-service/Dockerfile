FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Копируем модуль целиком, чтобы Gradle Wrapper мог собрать bootJar внутри контейнера
COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :auth-service:bootJar --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=builder /app/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
