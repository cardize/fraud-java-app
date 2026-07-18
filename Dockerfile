# syntax=docker/dockerfile:1
# Multi-stage build. One Dockerfile builds either bootable module:
#   docker build --build-arg MODULE=fraud-api .        (default)
#   docker build --build-arg MODULE=fraud-gateway .

########## build stage ##########
# Digest-pinned (external review finding J): a tag can silently move to a different image; the
# digest cannot. Dependabot's docker ecosystem keeps these pins bumped via PRs.
FROM maven:3-eclipse-temurin-26@sha256:d5617b9a6307e1b51dc7c55edf09bacb66f1c91fb861949c34a3a0d4e16bd241 AS build
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
FROM eclipse-temurin:21-jre@sha256:273396ed5998598ed1091e8d72711c2d36980a0e65103859c55a4e977a41ffd3
ARG MODULE=fraud-api
# curl exists only for the HEALTHCHECK below (the Ubuntu-based JRE image ships without it).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
# api: 8080 (+9090 management) · gateway: 8090 (+9091 management)
EXPOSE 8080 8090 9090 9091
# One Dockerfile serves both modules, whose management ports differ (api 9090, gateway 9091) —
# probe both; the first one that answers wins.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:9090/actuator/health || curl -fsS http://localhost:9091/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
