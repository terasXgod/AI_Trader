FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""

COPY core-service/build/libs/core-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
