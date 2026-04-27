FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""

COPY notification-service/build/libs/notification-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
