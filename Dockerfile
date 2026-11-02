# Build stage: the Gradle wrapper downloads Gradle; tests run separately (they need Docker for Testcontainers).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# Runtime stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/build/libs/orders.jar orders.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/orders.jar"]
