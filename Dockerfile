FROM maven:3.8.7-openjdk-18-slim

WORKDIR /usr/src/app
COPY . ./
RUN ./mvnw package

RUN cp target/gen-ai-text-classifier-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]