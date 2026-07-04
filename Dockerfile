# syntax=docker/dockerfile:1
# Multi-stage build. One Dockerfile builds either bootable module:
#   docker build --build-arg MODULE=fraud-api .        (default)
#   docker build --build-arg MODULE=fraud-gateway .

########## build stage ##########
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE=fraud-api
WORKDIR /workspace

# Copy ONLY the poms first: dependency resolution becomes its own cached layer, so source-only
# changes don't re-download the internet.
COPY pom.xml ./
COPY fraud-domain/pom.xml        fraud-domain/
COPY fraud-application/pom.xml   fraud-application/
COPY fraud-infrastructure/pom.xml fraud-infrastructure/
COPY fraud-api/pom.xml           fraud-api/
COPY fraud-gateway/pom.xml       fraud-gateway/
RUN mvn -B -q -pl ${MODULE} -am dependency:go-offline

COPY . .
# Tests run in CI, not during image build (keeps builds fast and reproducible).
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests

########## runtime stage ##########
FROM eclipse-temurin:21-jre
ARG MODULE=fraud-api
# Never run as root inside the container.
RUN useradd --system --uid 1001 appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
# api: 8080 (+9090 management) · gateway: 8090 (+9091 management)
EXPOSE 8080 8090 9090 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
