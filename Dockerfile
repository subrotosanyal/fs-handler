# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create directory for file system storage
RUN mkdir -p /data/fs-handler

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Environment variables with defaults
ENV SERVER_PORT=8080
ENV FILESYSTEM_TYPE=local
ENV FILESYSTEM_BASEPATH=/data/fs-handler
ENV FILESYSTEM_ROOT=.

# Expose the application port
EXPOSE ${SERVER_PORT}

# Set the entry point
ENTRYPOINT ["java", \
            "-jar", \
            "app.jar", \
            "--server.port=${SERVER_PORT}", \
            "--filesystem.type=${FILESYSTEM_TYPE}", \
            "--filesystem.basePath=${FILESYSTEM_BASEPATH}", \
            "--filesystem.root=${FILESYSTEM_ROOT}"]
