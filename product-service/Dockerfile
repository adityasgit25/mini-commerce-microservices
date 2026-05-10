# Use Java 21
FROM eclipse-temurin:21-jdk-jammy

# App jar location
ARG JAR_FILE=target/product-service-*.jar

# Copy jar into container
COPY ${JAR_FILE} app.jar

# Run application
ENTRYPOINT ["java", "-jar", "/app.jar"]