# First stage: Build the application
FROM eclipse-temurin:23-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/

# Ensure Maven wrapper is executable
RUN chmod +x mvnw

# Download dependencies for offline use
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw package -DskipTests

# Second stage: Run the application
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the necessary port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]