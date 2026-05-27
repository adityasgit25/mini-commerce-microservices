# Docker Compose and Minikube Testing Guide

This guide walks through testing the mini-commerce system first with Docker Compose, then deploying the same application to a Minikube Kubernetes cluster.

Run all commands from the project root:

```sh
/Users/adityamaheshwari/Documents/mini-commerce-system
```

## Stage 0: Build the Application

Build all Maven modules:

```sh
mvn clean package -DskipTests
```

Expected output near the end:

```text
Reactor Summary:
mini-commerce-system ... SUCCESS
common ............... SUCCESS
product-service ...... SUCCESS
user-service ......... SUCCESS
order-service ........ SUCCESS
discovery-server ..... SUCCESS
inventory-service .... SUCCESS
BUILD SUCCESS
```

If this fails, stop here. Docker and Kubernetes need the service jars to be built first.

## Stage 1: Validate Docker Compose

Validate the Compose file:

```sh
docker compose -f infra/docker/docker-compose.yml config --quiet
```

Expected output:

```text
```

No output means the Compose file is valid.

## Stage 2: Start the App with Docker Compose

Start everything:

```sh
docker compose -f infra/docker/docker-compose.yml up --build
```

Expected services:

- `mysql`
- `redis`
- `kafka`
- `zipkin`
- `discovery-server`
- `product-service`
- `order-service`
- `inventory-service`
- `user-service`
- `prometheus`

Expected logs should include lines like:

```text
mysql ... ready for connections
redis ... Ready to accept connections
kafka ... started
discovery-server ... Started DiscoveryServerApplication
product-service ... Started ProductServiceApplication
order-service ... Started OrderServiceApplication
inventory-service ... Started InventoryServiceApplication
user-service ... Started UserServiceApplication
```

In another terminal, check container status:

```sh
docker compose -f infra/docker/docker-compose.yml ps
```

Expected status:

```text
mysql              Up / healthy
redis              Up / healthy
kafka              Up / healthy
discovery-server   Up
product-service    Up
order-service      Up
inventory-service  Up
user-service       Up
prometheus         Up
zipkin             Up
```

## Stage 3: Check Eureka

Open Eureka in the browser:

```text
http://localhost:8761
```

Expected result:

The Eureka dashboard opens. After 30-90 seconds, you should see:

```text
PRODUCT-SERVICE
ORDER-SERVICE
INVENTORY-SERVICE
USER-SERVICE
```

## Stage 4: Check Service Health

Run:

```sh
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Expected output for each:

```json
{"status":"UP"}
```

## Stage 5: Test User Service

Create a user:

```sh
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Aditya","email":"aditya@example.com"}'
```

Expected output:

```json
{
  "id": 1,
  "name": "Aditya",
  "email": "aditya@example.com"
}
```

List users:

```sh
curl http://localhost:8081/users
```

Expected output:

```json
[
  {
    "id": 1,
    "name": "Aditya",
    "email": "aditya@example.com"
  }
]
```

## Stage 6: Test Product Service

Create a product:

```sh
curl -X POST http://localhost:8082/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"MacBook test product","price":120000}'
```

Expected output:

```json
{
  "id": 1,
  "name": "Laptop",
  "description": "MacBook test product",
  "price": 120000
}
```

List products:

```sh
curl http://localhost:8082/products
```

Expected output:

The response should contain the product you created.

## Stage 7: Test Order Service, Feign, and Kafka

Create an order:

```sh
curl -X POST "http://localhost:8083/orders?userId=1&productId=1&quantity=2"
```

Expected output:

```json
{
  "id": 1,
  "userId": 1,
  "productId": 1,
  "quantity": 2,
  "totalPrice": 240000
}
```

This verifies:

- order-service API
- order-service database connection
- Feign call from order-service to product-service
- Kafka producer sending `OrderCreatedEvent`

Check inventory-service logs:

```sh
docker logs inventory-service
```

Expected log:

```text
Inventory Service received event: ...
Reducing stock for product: 1
```

If you see that log, the Kafka flow is working.

## Stage 8: Check Prometheus and Zipkin

Open Prometheus:

```text
http://localhost:9090
```

Go to **Status -> Targets**.

Expected targets:

```text
order-service      UP
product-service    UP
inventory-service  UP
user-service       UP
```

Open Zipkin:

```text
http://localhost:9411
```

Expected result:

Zipkin UI opens. After API calls, traces may appear for service requests.

## Stage 9: Stop Docker Compose

Stop containers:

```sh
docker compose -f infra/docker/docker-compose.yml down
```

Stop containers and remove MySQL data:

```sh
docker compose -f infra/docker/docker-compose.yml down -v
```

Use `-v` only when you want a clean database next time.

## Stage 10: Start Minikube

Start Minikube:

```sh
minikube start
```

Expected output:

```text
Done! kubectl is now configured to use "minikube" cluster
```

Check the node:

```sh
kubectl get nodes
```

Expected output:

```text
NAME       STATUS   ROLES           AGE   VERSION
minikube   Ready    control-plane   ...
```

## Stage 11: Point Docker to Minikube

Use Minikube's Docker daemon:

```sh
eval $(minikube docker-env)
```

Check current images:

```sh
docker images | grep mini-commerce
```

At first, this may show nothing. That is okay.

## Stage 12: Build Images Inside Minikube

Build the jars again if needed:

```sh
mvn clean package -DskipTests
```

Build Docker images inside Minikube:

```sh
docker compose -f infra/docker/docker-compose.yml build
```

Check images:

```sh
docker images | grep mini-commerce
```

Expected images:

```text
mini-commerce/discovery-server
mini-commerce/product-service
mini-commerce/order-service
mini-commerce/inventory-service
mini-commerce/user-service
```

This step is important because Kubernetes uses local Minikube Docker images with:

```yaml
imagePullPolicy: IfNotPresent
```

## Stage 13: Apply Kubernetes Manifests

Deploy the app:

```sh
kubectl apply -f infra/k8s/
```

Expected output:

```text
secret/mysql-secret created
configmap/mysql-init created
configmap/app-config created
persistentvolumeclaim/mysql-data created
deployment.apps/mysql created
service/mysql created
deployment.apps/redis created
service/redis created
deployment.apps/kafka created
service/kafka created
deployment.apps/zipkin created
service/zipkin created
deployment.apps/discovery-server created
service/discovery-server created
deployment.apps/product-service created
service/product-service created
deployment.apps/order-service created
service/order-service created
deployment.apps/inventory-service created
service/inventory-service created
deployment.apps/user-service created
service/user-service created
```

If you already applied it once, Kubernetes may print:

```text
configured
```

That is also fine.

## Stage 14: Watch Pods

Watch pods:

```sh
kubectl get pods -w
```

Expected progression:

```text
mysql              0/1 ContainerCreating
mysql              1/1 Running
redis              1/1 Running
kafka              1/1 Running
zipkin             1/1 Running
discovery-server   1/1 Running
product-service    1/1 Running
order-service      1/1 Running
inventory-service  1/1 Running
user-service       1/1 Running
```

Some pods may show:

```text
0/1 Running
```

for a while until readiness probes pass. That is normal.

## Stage 15: Check Kubernetes Services

Run:

```sh
kubectl get svc
```

Expected services:

```text
discovery-server   NodePort   8761:30761/TCP
user-service       NodePort   8081:30081/TCP
product-service    NodePort   8082:30082/TCP
order-service      NodePort   8083:30083/TCP
inventory-service  NodePort   8084:30084/TCP
zipkin             NodePort   9411:30411/TCP
mysql              ClusterIP  3306/TCP
redis              ClusterIP  6379/TCP
kafka              ClusterIP  9092/TCP
```

## Stage 16: Open Services Through Minikube

Open Eureka:

```sh
minikube service discovery-server
```

Expected result:

The Eureka dashboard opens in the browser.

Get service URLs:

```sh
minikube service user-service --url
minikube service product-service --url
minikube service order-service --url
```

Expected output:

```text
http://127.0.0.1:<some-port>
```

Save URLs into shell variables:

```sh
USER_URL=$(minikube service user-service --url)
PRODUCT_URL=$(minikube service product-service --url)
ORDER_URL=$(minikube service order-service --url)
```

Test user-service:

```sh
curl -X POST "$USER_URL/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Aditya","email":"aditya@example.com"}'
```

Test product-service:

```sh
curl -X POST "$PRODUCT_URL/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"K8s test product","price":120000}'
```

Test order-service:

```sh
curl -X POST "$ORDER_URL/orders?userId=1&productId=1&quantity=2"
```

Expected result:

The API responses should match the Docker Compose tests.

## Stage 17: Debug Common Failures

Check pods:

```sh
kubectl get pods
```

Check logs:

```sh
kubectl logs deployment/discovery-server
kubectl logs deployment/product-service
kubectl logs deployment/order-service
kubectl logs deployment/inventory-service
kubectl logs deployment/user-service
kubectl logs deployment/kafka
kubectl logs deployment/mysql
```

Describe a failing pod:

```sh
kubectl describe pod <pod-name>
```

### ImagePullBackOff

Usually means the image was not built inside Minikube Docker.

Fix:

```sh
eval $(minikube docker-env)
docker compose -f infra/docker/docker-compose.yml build
kubectl rollout restart deployment/product-service deployment/order-service deployment/inventory-service deployment/user-service deployment/discovery-server
```

### CrashLoopBackOff

Check logs:

```sh
kubectl logs deployment/<deployment-name>
```

Common causes:

- database connection issue
- Kafka connection issue
- Eureka connection issue
- application configuration issue

### 0/1 Running

The container is running, but readiness probe is not passing yet.

Wait a bit, then check logs:

```sh
kubectl logs deployment/<deployment-name>
```

## Stage 18: Clean Up Kubernetes

Delete app resources:

```sh
kubectl delete -f infra/k8s/
```

Delete the full Minikube cluster:

```sh
minikube delete
```

## Recommended Full Command Order

Use this order for Docker Compose:

```sh
mvn clean package -DskipTests
docker compose -f infra/docker/docker-compose.yml config --quiet
docker compose -f infra/docker/docker-compose.yml up --build
```

After Compose testing is done:

```sh
docker compose -f infra/docker/docker-compose.yml down
```

Use this order for Minikube:

```sh
minikube start
eval $(minikube docker-env)
mvn clean package -DskipTests
docker compose -f infra/docker/docker-compose.yml build
kubectl apply -f infra/k8s/
kubectl get pods -w
```

Once all pods are running, test APIs with:

```sh
minikube service <service-name> --url
```
