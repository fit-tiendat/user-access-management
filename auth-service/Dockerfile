FROM eclipse-temurin:17-jre
WORKDIR /app
# copy đúng duy nhất file jar (không copy original-*.jar)
ARG JAR_FILE=target/auth-service-*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
