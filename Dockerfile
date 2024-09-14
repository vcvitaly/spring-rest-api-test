# Stage 1: Build the application
FROM amazoncorretto:21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the build.gradle and settings.gradle (if any) into the container
COPY build.gradle settings.gradle /app/

# Copy the Gradle wrapper into the container
COPY gradlew /app/
COPY gradle /app/gradle

# Download the Gradle dependencies
RUN ./gradlew dependencies

# Copy the rest of the application source code into the container
COPY src /app/src

# Build the application
RUN ./gradlew build

# Stage 2: Create the runtime image
FROM amazoncorretto:21-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built application from the previous stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]