FROM openjdk:11-jre-slim

RUN mkdir -p /app
WORKDIR /app

ADD build/libs/app.jar /app

CMD ["java", "-Djava.net.preferIPv4Stack=true", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
