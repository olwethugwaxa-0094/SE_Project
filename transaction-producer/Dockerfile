FROM eclipse-temurin:21-jre
WORKDIR /app
COPY transaction-producer/target/transaction-producer-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
