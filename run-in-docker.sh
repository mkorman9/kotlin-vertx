#!/bin/bash

./gradlew build
docker build -t mkorman/kotlin-vertx .
docker run -it --rm \
  -p 8080:8080 \
  -v $(pwd)/config-docker.yml:/app/config.yml:ro \
  -e AWS_ACCESS_KEY=xxx \
  -e AWS_SECRET_ACCESS_KEY=yyy \
  --net kotlin-vertx_default \
  mkorman/kotlin-vertx
