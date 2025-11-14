# --------- build stage (tạo JAR) ---------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy POM cha + module cần thiết trước để cache
COPY pom.xml ./pom.xml
COPY core/pom.xml ./core/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
# copy source
COPY core ./core
COPY auth-service ./auth-service
COPY user-service ./user-service
# build riêng module auth-service (kéo theo core) và bỏ test
RUN mvn -q -DskipTests -pl auth-service -am package

# --------- runtime stage (chạy app) ---------
FROM eclipse-temurin:17-jre
WORKDIR /app
# copy đúng file jar đã build từ stage trên
COPY --from=build /workspace/auth-service/target/auth-service-*-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
