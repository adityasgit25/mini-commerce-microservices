# Mini Commerce on kind

This folder is the kind-specific Kubernetes deployment. Keep the minikube manifests in `infra/k8s/` and make kind-only changes here.

## 1. Create the kind cluster

```sh
kind create cluster --config infra/k8s-kind/cluster/kind-cluster.yml
kubectl cluster-info --context kind-mini-commerce-kind
```

Switch back to this cluster later with:

```sh
kubectl config use-context kind-mini-commerce-kind
```

## 2. Build the application jars

```sh
mvn clean package -DskipTests
```

## 3. Build Docker images locally

Unlike minikube, kind does not use `eval $(minikube docker-env)`. Build the images in your local Docker daemon:

```sh
docker compose -f infra/docker/docker-compose.yml build discovery-server product-service order-service inventory-service user-service
```

## 4. Load local images into kind

```sh
kind load docker-image mini-commerce/discovery-server:latest --name mini-commerce-kind
kind load docker-image mini-commerce/product-service:latest --name mini-commerce-kind
kind load docker-image mini-commerce/order-service:latest --name mini-commerce-kind
kind load docker-image mini-commerce/inventory-service:latest --name mini-commerce-kind
kind load docker-image mini-commerce/user-service:latest --name mini-commerce-kind
```

## 5. Deploy

```sh
kubectl apply -f infra/k8s-kind/
kubectl get pods
kubectl get svc
```

## 6. Open services

The `cluster/kind-cluster.yml` file maps these NodePorts to localhost:

- user-service: http://localhost:30081
- product-service: http://localhost:30082
- order-service: http://localhost:30083
- inventory-service: http://localhost:30084
- zipkin: http://localhost:30411
- discovery-server: http://localhost:30761

## Useful commands

```sh
kubectl rollout status deployment/discovery-server
kubectl rollout status deployment/product-service
kubectl logs deployment/product-service
kubectl describe pod -l app=product-service
```

Delete the kind cluster when you are done:

```sh
kind delete cluster --name mini-commerce-kind
```
