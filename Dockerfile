FROM openjdk:11-jre-slim

WORKDIR /
ADD build/libs/app.jar /

CMD ["java", "-jar", "/app.jar"]
