#!/bin/bash

./gradlew build
docker build -t mkorman/kotlin-vertx .
docker run -it --rm -p 8080:8080 -v $(pwd)/config-docker.yml:/config.yml:ro --net kotlin-vertx_default mkorman/kotlin-vertx
