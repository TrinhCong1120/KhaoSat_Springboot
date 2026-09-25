FROM maven:3.9-eclipse-temurin-17 AS build

ENV MAVEN_OPTS="-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false"

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

ENV JAVA_OPTS="-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false"

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
