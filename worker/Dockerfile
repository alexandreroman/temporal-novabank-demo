FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY --link pom.xml mvnw ./
COPY --link .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY --link src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --link --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
