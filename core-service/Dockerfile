<<<<<<< HEAD
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Копируем модуль целиком, чтобы Gradle Wrapper мог собрать bootJar внутри контейнера
COPY core-service/ ./

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon


=======
>>>>>>> 5c38a76 (feat: non-worked service((()
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""

<<<<<<< HEAD
COPY --from=builder /app/build/libs/core-service-0.0.1-SNAPSHOT.jar app.jar
=======
COPY core-service/build/libs/core-service-0.0.1-SNAPSHOT.jar app.jar
>>>>>>> 5c38a76 (feat: non-worked service((()

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
