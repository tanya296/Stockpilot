# ---------- Stage 1: build ----------
# A big image with the full JDK and Maven, used only to compile the app.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first and download dependencies. Docker caches each step (layer),
# so as long as pom.xml doesn't change, later builds skip this slow download.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source and build the jar. Tests run in CI, so they're skipped here.
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: run ----------
# A small image with only the Java runtime (no JDK, no Maven, no source code).
# Smaller image = faster deploys and less attack surface.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root inside the container.
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
