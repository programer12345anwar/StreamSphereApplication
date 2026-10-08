# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY service-registry/pom.xml ./service-registry/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY central/pom.xml ./central/pom.xml
COPY video-service/pom.xml ./video-service/pom.xml
COPY notification-api/pom.xml ./notification-api/pom.xml
RUN mvn -q -pl service-registry -am -DskipTests dependency:go-offline
COPY service-registry/src ./service-registry/src
RUN mvn -q -pl service-registry -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /app/service-registry/target/service-registry-0.0.1-SNAPSHOT.jar app.jar
USER app
ENV JAVA_OPTS=""
EXPOSE 8761
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
