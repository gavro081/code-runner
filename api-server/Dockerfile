# multi-stage build for api-server.
# build context is the repository ROOT because api-server's
# parent is the root reactor pom and it depends on the `common` module.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy poms first so dependency resolution can be cached independently of source changes
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY api-server/pom.xml api-server/pom.xml

# build and install `common` to the local repo (api-server depends on it)
COPY common/src common/src
RUN mvn -f common/pom.xml -B clean install -DskipTests

# build the api-server jar. parent pom is resolved from ../pom.xml (relativePath)
COPY api-server/src api-server/src
RUN mvn -f api-server/pom.xml -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/api-server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
