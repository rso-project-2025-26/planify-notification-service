FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy parent POM and build files
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# Copy notification-core module
COPY notification-core/pom.xml notification-core/
COPY notification-core/src notification-core/src

# Copy notification-functions module (needed for parent POM validation)
COPY notification-functions/pom.xml notification-functions/

# Build only the core module (Spring Boot app)
RUN chmod +x ./mvnw && ./mvnw -q -e -DskipTests clean package -pl notification-core -am

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/notification-core/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]