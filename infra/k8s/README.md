# Mini Commerce on Minikube

Build the jars first:

```sh
mvn clean package -DskipTests
```

For Docker Compose:

```sh
docker compose -f infra/docker/docker-compose.yml up --build
```

For Minikube, build the images inside Minikube's Docker daemon:

```sh
minikube start
eval $(minikube docker-env)
docker compose -f infra/docker/docker-compose.yml build
kubectl apply -f infra/k8s/
kubectl get pods
```

Open services through Minikube:

```sh
minikube service discovery-server
minikube service product-service
minikube service order-service
minikube service user-service
```

Useful NodePorts:

- user-service: `30081`
- product-service: `30082`
- order-service: `30083`
- inventory-service: `30084`
- discovery-server: `30761`
- zipkin: `30411`
