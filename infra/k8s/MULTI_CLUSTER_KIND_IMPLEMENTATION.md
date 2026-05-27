# Multi-cluster Kind Implementation Guide

This guide explains how to implement and use the local multi-cluster setup for
the mini-commerce system.

The goal is to run the same Kubernetes application stack in two separate Kind
clusters on your laptop:

- `mini-commerce-a`
- `mini-commerce-b`

This is a learning-friendly first version of multi-cluster. Both clusters are
independent. They do not call each other yet.

## What You Will Build

You will create:

- Two Kind cluster config files.
- A deployment script that builds the app and deploys it to both clusters.
- A cleanup script that deletes both clusters.
- Separate localhost ports for each cluster so both can run together.

## File Layout

```text
infra/
  kind/
    cluster-a.yml
    cluster-b.yml
  k8s/
    00-config.yml
    01-mysql.yml
    02-redis.yml
    03-kafka.yml
    04-zipkin.yml
    10-discovery-server.yml
    20-product-service.yml
    21-order-service.yml
    22-inventory-service.yml
    23-user-service.yml
scripts/
  deploy-kind-multicluster.sh
  delete-kind-multicluster.sh
```

## Step 1: Create a Branch

Start from your existing Kind deployment branch:

```sh
git switch kind-deployment
git switch -c kind-multi-cluster
```

If the branch already exists:

```sh
git switch kind-multi-cluster
```

## Step 2: Install Prerequisites

You need:

- Docker
- Kind
- kubectl
- Maven

On macOS with Homebrew:

```sh
brew install kind kubectl maven
```

Check them:

```sh
docker --version
kind --version
kubectl version --client
mvn --version
```

## Step 3: Understand the Cluster Configs

Kind runs Kubernetes clusters inside Docker containers. The files in
`infra/kind/` define two separate clusters.

Cluster A maps Kubernetes NodePorts to localhost ports starting with `18`:

```text
user-service       localhost:18081
product-service    localhost:18082
order-service      localhost:18083
inventory-service  localhost:18084
discovery-server   localhost:18761
zipkin             localhost:19411
```

Cluster B maps the same Kubernetes NodePorts to localhost ports starting with
`28`:

```text
user-service       localhost:28081
product-service    localhost:28082
order-service      localhost:28083
inventory-service  localhost:28084
discovery-server   localhost:28761
zipkin             localhost:29411
```

The Kubernetes manifests still use the same NodePorts internally, such as
`30081` for `user-service`. The Kind configs decide which localhost port reaches
that NodePort for each cluster.

## Step 4: Create Both Clusters Manually

You can create the clusters manually:

```sh
kind create cluster --name mini-commerce-a --config infra/kind/cluster-a.yml
kind create cluster --name mini-commerce-b --config infra/kind/cluster-b.yml
```

Check contexts:

```sh
kubectl config get-contexts
```

You should see:

```text
kind-mini-commerce-a
kind-mini-commerce-b
```

## Step 5: Build the Application

Build the Spring Boot jars:

```sh
mvn clean package -DskipTests
```

Build the local Docker images:

```sh
docker compose -f infra/docker/docker-compose.yml build \
  discovery-server \
  product-service \
  order-service \
  inventory-service \
  user-service
```

## Step 6: Load Images Into Both Kind Clusters

Kind clusters cannot automatically see images from your local Docker daemon.
You must load each local app image into each cluster:

```sh
kind load docker-image mini-commerce/discovery-server:latest --name mini-commerce-a
kind load docker-image mini-commerce/product-service:latest --name mini-commerce-a
kind load docker-image mini-commerce/order-service:latest --name mini-commerce-a
kind load docker-image mini-commerce/inventory-service:latest --name mini-commerce-a
kind load docker-image mini-commerce/user-service:latest --name mini-commerce-a

kind load docker-image mini-commerce/discovery-server:latest --name mini-commerce-b
kind load docker-image mini-commerce/product-service:latest --name mini-commerce-b
kind load docker-image mini-commerce/order-service:latest --name mini-commerce-b
kind load docker-image mini-commerce/inventory-service:latest --name mini-commerce-b
kind load docker-image mini-commerce/user-service:latest --name mini-commerce-b
```

The manifests use:

```yaml
imagePullPolicy: IfNotPresent
```

That lets Kubernetes use the image loaded into the Kind node.

## Step 7: Deploy to Both Clusters

Apply the same manifests to both clusters:

```sh
kubectl --context kind-mini-commerce-a apply -f infra/k8s/
kubectl --context kind-mini-commerce-b apply -f infra/k8s/
```

Wait for rollouts:

```sh
kubectl --context kind-mini-commerce-a rollout status deployment/mysql
kubectl --context kind-mini-commerce-a rollout status deployment/redis
kubectl --context kind-mini-commerce-a rollout status deployment/kafka
kubectl --context kind-mini-commerce-a rollout status deployment/discovery-server
kubectl --context kind-mini-commerce-a rollout status deployment/product-service
kubectl --context kind-mini-commerce-a rollout status deployment/order-service
kubectl --context kind-mini-commerce-a rollout status deployment/inventory-service
kubectl --context kind-mini-commerce-a rollout status deployment/user-service

kubectl --context kind-mini-commerce-b rollout status deployment/mysql
kubectl --context kind-mini-commerce-b rollout status deployment/redis
kubectl --context kind-mini-commerce-b rollout status deployment/kafka
kubectl --context kind-mini-commerce-b rollout status deployment/discovery-server
kubectl --context kind-mini-commerce-b rollout status deployment/product-service
kubectl --context kind-mini-commerce-b rollout status deployment/order-service
kubectl --context kind-mini-commerce-b rollout status deployment/inventory-service
kubectl --context kind-mini-commerce-b rollout status deployment/user-service
```

## Step 8: Use the Automation Script

Instead of running all commands manually, use:

```sh
./scripts/deploy-kind-multicluster.sh
```

The script:

1. Checks required tools.
2. Builds jars.
3. Builds Docker images.
4. Creates both Kind clusters if missing.
5. Loads app images into both clusters.
6. Applies the Kubernetes manifests.
7. Waits for deployments to roll out.

## Step 9: Verify the Deployment

Check pods in cluster A:

```sh
kubectl --context kind-mini-commerce-a get pods
kubectl --context kind-mini-commerce-a get svc
```

Check pods in cluster B:

```sh
kubectl --context kind-mini-commerce-b get pods
kubectl --context kind-mini-commerce-b get svc
```

Open cluster A:

```sh
open http://localhost:18761
open http://localhost:19411
```

Open cluster B:

```sh
open http://localhost:28761
open http://localhost:29411
```

Use `curl` for service checks:

```sh
curl http://localhost:18081/actuator/health
curl http://localhost:28081/actuator/health
```

## Step 10: Switch Contexts While Learning

Prefer explicit contexts:

```sh
kubectl --context kind-mini-commerce-a get pods
kubectl --context kind-mini-commerce-b get pods
```

You can also switch the default context:

```sh
kubectl config use-context kind-mini-commerce-a
kubectl get pods

kubectl config use-context kind-mini-commerce-b
kubectl get pods
```

## Step 11: Troubleshooting

If a pod is not ready:

```sh
kubectl --context kind-mini-commerce-a describe pod <pod-name>
kubectl --context kind-mini-commerce-a logs deployment/<deployment-name>
```

For cluster B, change the context:

```sh
kubectl --context kind-mini-commerce-b describe pod <pod-name>
kubectl --context kind-mini-commerce-b logs deployment/<deployment-name>
```

If an app image is not found, rebuild and reload it:

```sh
docker compose -f infra/docker/docker-compose.yml build product-service
kind load docker-image mini-commerce/product-service:latest --name mini-commerce-a
kind load docker-image mini-commerce/product-service:latest --name mini-commerce-b
```

Then restart the deployment:

```sh
kubectl --context kind-mini-commerce-a rollout restart deployment/product-service
kubectl --context kind-mini-commerce-b rollout restart deployment/product-service
```

If a localhost port is already used, edit:

```text
infra/kind/cluster-a.yml
infra/kind/cluster-b.yml
```

Then recreate the clusters:

```sh
./scripts/delete-kind-multicluster.sh
./scripts/deploy-kind-multicluster.sh
```

## Step 12: Cleanup

Delete both clusters:

```sh
./scripts/delete-kind-multicluster.sh
```

Or manually:

```sh
kind delete cluster --name mini-commerce-a
kind delete cluster --name mini-commerce-b
```

## Next Learning Steps

After this works, continue in small steps:

1. Deploy only infrastructure services to cluster A and app services to cluster B.
2. Add ingress or an API gateway for each cluster.
3. Add Prometheus per cluster and compare metrics.
4. Try GitOps with Argo CD or Flux.
5. Try cross-cluster networking with Istio, Linkerd, or Cilium Cluster Mesh.

The important idea is this: multi-cluster starts with strong context discipline.
Always know which cluster you are applying to, checking, and debugging.
