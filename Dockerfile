FROM eclipse-temurin:11

RUN mkdir -p /app
WORKDIR /app

ADD build/libs/app.jar /app

CMD ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
