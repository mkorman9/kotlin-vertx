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

## Deploy to Kubernetes on production

Requires Helm

### Nginx ingress controller
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

Add additional security headers
```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: security-headers
  namespace: ingress-nginx
data:
  X-Frame-Options: "DENY"
  X-Content-Type-Options: "nosniff"
  X-XSS-Protection: "0"
  Strict-Transport-Security: "max-age=63072000; includeSubDomains; preload"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: ingress-nginx-controller
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
data:
  add-headers: "ingress-nginx/security-headers"
EOF
```

### RabbitMQ cluster operator

Create RabbitMQ cluster operator
```bash
kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"
```

### Project namespace

Create a namespace for the project
```bash
kubectl create namespace kotlin-vertx
kubectl config set-context --current --namespace=kotlin-vertx
```

### Access to Gitlab registry

- Generate a Gitlab personal access token with `read_registry` scope
- Generate AUTH_STRING with `echo -n '<USERNAME>:<ACCESS_TOKEN>' | base64`
- Create a `docker.json` file
```
{
    "auths": {
        "registry.gitlab.com": {
            "auth": "<AUTH_STRING>"
        }
    }
}
```

Upload it to the cluster
```bash
kubectl create secret generic gitlab-docker-registry --namespace=kube-system \
--from-file=.dockerconfigjson=./docker.json --type="kubernetes.io/dockerconfigjson"
```

### RabbitMQ cluster

Create `broker.yml` file. Adjust replicas count
```
apiVersion: rabbitmq.com/v1beta1
kind: RabbitmqCluster
metadata:
  name: broker
spec:
  replicas: 1
```

Deploy it
```
kubectl apply -f broker.yml
```

Retrieve default username and password
```bash
kubectl get secret broker-default-user -o jsonpath='{.data.username}' | base64 --decode
kubectl get secret broker-default-user -o jsonpath='{.data.password}' | base64 --decode
```

URL can be constructed from retrieved data
```
amqp://<username>:<password>@broker.kotlin-vertx.svc.cluster.local:5672
```

### App secrets

Create `secrets.yml` file and populate it with data
```
db:
  uri: jdbc:postgresql://<POSTGRES_HOST>:5432/<POSTGRES_DB_NAME>
  user: <POSTGRES_USERNAME>
  password: <POSTGRES_PASSWORD>
rabbitmq:
  uri: <RABBITMQ_URL>
```

Upload it
```bash
kubectl create secret generic secrets --from-file=secrets.yml
```

### TLS certificate

Either generate a self-signed cert

```bash
export DOMAIN="example.com"
openssl req -x509 -nodes -days 365 -newkey rsa:4096 -keyout key.pem -out cert.pem -subj "/CN=$DOMAIN/O=$DOMAIN"
```

Or use Let's Encrypt to generate a proper one
```bash
# brew install certbot
export DOMAIN="example.com"
sudo certbot -d "$DOMAIN" --manual --preferred-challenges dns certonly
sudo cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ./cert.pem && sudo chown $USER ./cert.pem
sudo cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" ./key.pem && sudo chown $USER ./key.pem

# to renew later: sudo certbot renew -q
```

Upload it
```bash
kubectl create secret tls domain-specific-tls-cert --key key.pem --cert cert.pem
```

### Deploy

Set proper values for flags and run
```bash
deployment/deploy.sh \
  --set app.version="v1.0.4" \
  --set app.imageName="registry.gitlab.com/mkorman/kotlin-vertx" \
  --set images.pullSecret="kube-system/gitlab-docker-registry" \
  --set ingress.hostname="example.com" \
  --set ingress.useHttps=true \
  --set ingress.tlsCertName="domain-specific-tls-cert"
```

Undeploy the app later
```bash
deployment/undeploy.sh
```
