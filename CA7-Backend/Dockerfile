FROM maven:3.8.1-openjdk-11 as build
WORKDIR /tmp/
COPY pom.xml /tmp
COPY src /tmp/src
RUN mvn -Dhttps.protocols=TLSv1.2 package

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /tmp/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]