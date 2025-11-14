# --------- build stage (tạo JAR) ---------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY core/pom.xml ./core/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

COPY core ./core
COPY user-service ./user-service
COPY auth-service ./auth-service
RUN mvn -q -DskipTests -pl user-service -am package

# --------- runtime stage (chạy app) ---------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service-*-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
