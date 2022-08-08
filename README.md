Personal playground for Kotlin and Vert.x

## Local run

Requires JDK 11

Build
```bash
./gradlew build
```

Start external dependencies
```bash
docker-compose up
```

Start the app
```bash
java -jar build/libs/app.jar
```

Optionally upload some random data for tests
```bash
testdata/upload.sh
```

Cleanup docker-compose stack:
```bash
docker-compose down
```
