# Mini Commerce on Kubernetes

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

## Multi-cluster with Kind

Detailed implementation guide:
[MULTI_CLUSTER_KIND_IMPLEMENTATION.md](./MULTI_CLUSTER_KIND_IMPLEMENTATION.md)

This repo includes a local two-cluster Kind setup for learning multi-cluster
operations. It deploys the same mini-commerce stack into two separate clusters:

- `mini-commerce-a`
- `mini-commerce-b`

This is intentionally the first step before cross-cluster service discovery or a
service mesh. You learn how to create multiple clusters, switch contexts, load
local images into each cluster, deploy the same manifests twice, and compare
rollouts independently.

### Create a branch

If you are starting from another branch:

```sh
git switch kind-deployment
git switch -c kind-multi-cluster
```

### Prerequisites

Install these locally:

- Docker
- Kind
- kubectl
- Maven

On macOS with Homebrew:

```sh
brew install kind kubectl maven
```

### Deploy both clusters

From the repository root:

```sh
./scripts/deploy-kind-multicluster.sh
```

The script does four things:

1. Builds the Spring Boot jars.
2. Builds the local application Docker images.
3. Creates both Kind clusters from `infra/kind/cluster-a.yml` and
   `infra/kind/cluster-b.yml`.
4. Loads the application images into both clusters and applies `infra/k8s/`.

### Check each cluster

```sh
kubectl config get-contexts
kubectl --context kind-mini-commerce-a get pods
kubectl --context kind-mini-commerce-b get pods
```

Useful local ports for cluster A:

- user-service: `http://localhost:18081`
- product-service: `http://localhost:18082`
- order-service: `http://localhost:18083`
- inventory-service: `http://localhost:18084`
- discovery-server: `http://localhost:18761`
- zipkin: `http://localhost:19411`

Useful local ports for cluster B:

- user-service: `http://localhost:28081`
- product-service: `http://localhost:28082`
- order-service: `http://localhost:28083`
- inventory-service: `http://localhost:28084`
- discovery-server: `http://localhost:28761`
- zipkin: `http://localhost:29411`

### Switch your default context

Use explicit `--context` while learning. When you want one cluster as the
default:

```sh
kubectl config use-context kind-mini-commerce-a
kubectl get pods
```

Then switch to the second cluster:

```sh
kubectl config use-context kind-mini-commerce-b
kubectl get pods
```

### Delete both clusters

```sh
./scripts/delete-kind-multicluster.sh
```

### What to learn next

After this works, the natural next steps are:

1. Split responsibilities, for example infrastructure services in cluster A and
   application services in cluster B.
2. Add a gateway or ingress per cluster.
3. Try cross-cluster networking with a service mesh such as Istio, Linkerd, or
   Cilium Cluster Mesh.
4. Add GitOps tooling so both clusters reconcile from the same manifests.
