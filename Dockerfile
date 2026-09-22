ARG SERVICE_DIR

FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE_DIR

ENV MAVEN_OPTS="-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false"

WORKDIR /workspace
COPY ${SERVICE_DIR}/pom.xml ${SERVICE_DIR}/pom.xml

WORKDIR /workspace/${SERVICE_DIR}
RUN mvn -B -DskipTests dependency:go-offline

COPY ${SERVICE_DIR}/src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
ARG SERVICE_DIR

ENV JAVA_OPTS="-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false"

WORKDIR /app
COPY --from=build /workspace/${SERVICE_DIR}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
