FROM gradle:8.10-jdk17-jammy AS build
WORKDIR /workspace

# Cache dependencies by copying Gradle metadata first
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
RUN chmod +x gradlew
RUN ./gradlew dependencies > /dev/null 2>&1 || true

# Copy the remaining project files and build the Spring Boot jar
COPY . .
RUN ./gradlew bootJar -x test && \
    mkdir -p build/output && \
    cp build/libs/*SNAPSHOT.jar build/output/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/build/output/app.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
