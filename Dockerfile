FROM openjdk:11-jre-slim

WORKDIR /
ADD build/libs/app-*-shadow.jar /app.jar

CMD ["java", "-jar", "/app.jar"]
