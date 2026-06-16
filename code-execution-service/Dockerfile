# multi-stage build for code-execution-service
# build context is the repository ROOT (see docker-compose.yml): this service's parent is
# the root reactor pom and it depends on the `common` module
#
# NOTE: this service talks to the Docker daemon via the docker-java SDK over a mounted
# /var/run/docker.sock. it does NOT need the docker CLI inside the image, and it runs as
# root (default) so it can access the mounted socket. at startup it builds the python/js
# sandbox images on the host daemon if they are missing.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy poms first for dependency-resolution caching.
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY code-execution-service/pom.xml code-execution-service/pom.xml

# build and install `common` to the local repo (this service depends on it)
COPY common/src common/src
RUN mvn -f common/pom.xml -B clean install -DskipTests

# build the code-execution-service jar. parent pom resolved from ../pom.xml (relativePath)
COPY code-execution-service/src code-execution-service/src
RUN mvn -f code-execution-service/pom.xml -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/code-execution-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
