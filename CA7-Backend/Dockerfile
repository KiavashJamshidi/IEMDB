FROM maven:3.8.5-openjdk-17 as MAVEN
MAINTAINER IEMDB_DEVELOPER
COPY pom.xml /build/
COPY src /build/src/
WORKDIR /build/
RUN mvn package

FROM openjdk:17
WORKDIR /app
COPY --from=MAVEN /build/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]