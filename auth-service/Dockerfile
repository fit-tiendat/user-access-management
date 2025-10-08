# ----- build -----
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY core/pom.xml core/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -q -DskipTests -pl auth-service -am dependency:go-offline
COPY core/ core/
COPY auth-service/ auth-service/
RUN mvn -q -DskipTests -pl auth-service -am package

# ----- run -----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/auth-service/target/*-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
