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

Cleanup docker-compose stack:
```bash
docker-compose down && rm -rf _docker_compose_volumes
```

## Deploy to Kubernetes in Docker Desktop

Requires Helm and Docker Desktop with Kubernetes running
     
Create nginx ingress controller
```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

kubectl create namespace ingress-nginx

helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace=ingress-nginx \
  --set controller.service.externalTrafficPolicy="Local" \
  --set controller.replicaCount=1
  
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

Create RabbitMQ cluster operator
```bash
kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"
```

Create a namespace for the project
```bash
kubectl create namespace kotlin-vertx
kubectl config set-context --current --namespace=kotlin-vertx
```

Build app's image
```bash
deployment/build.sh
```

Create development environment for the app (unsuitable for production)
```bash
deployment/kubernetes-env/up.sh
```

Deploy the app
```bash
deployment/deploy.sh
```

Test with `curl -v http://localhost/api/v1/client`

Undeploy the app
```bash
deployment/undeploy.sh
```

Shut down the environment
```bash
deployment/kubernetes-env/down.sh
```
