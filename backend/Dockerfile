FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/contacts-api-0.0.1-SNAPSHOT.jar app.jar
ENV PICTURE_DIR=/data/pictures
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
