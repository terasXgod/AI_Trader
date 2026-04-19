FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""

COPY auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
