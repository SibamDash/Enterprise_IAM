# ---- build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Windows format line endings might cause issues, make sure it's executable
RUN sed -i 's/\r$//' mvnw
RUN chmod +x ./mvnw
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S iam && adduser -S iam -G iam
COPY --from=build /app/target/*.jar app.jar
USER iam
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
