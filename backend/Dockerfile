FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S ticketflow && adduser -S ticketflow -G ticketflow

COPY --from=build /workspace/target/ticketflow-backend-*.jar /app/app.jar

USER ticketflow

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
