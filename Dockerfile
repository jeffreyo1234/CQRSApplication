# Use an official Maven image with Eclipse Temurin JDK 21 to build the application
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Use an official Amazon Corretto runtime as a parent image
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /app/target/event-driven-cqrs-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]