#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K8S_DIR="${ROOT_DIR}/infra/k8s"
COMPOSE_FILE="${ROOT_DIR}/infra/docker/docker-compose.yml"

CLUSTERS=(
  "mini-commerce-a:${ROOT_DIR}/infra/kind/cluster-a.yml"
  "mini-commerce-b:${ROOT_DIR}/infra/kind/cluster-b.yml"
)

APP_IMAGES=(
  "mini-commerce/discovery-server:latest"
  "mini-commerce/product-service:latest"
  "mini-commerce/order-service:latest"
  "mini-commerce/inventory-service:latest"
  "mini-commerce/user-service:latest"
)

DEPLOYMENTS=(
  mysql
  redis
  kafka
  zipkin
  discovery-server
  product-service
  order-service
  inventory-service
  user-service
)

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

cluster_exists() {
  kind get clusters | grep -qx "$1"
}

create_cluster() {
  local name="$1"
  local config="$2"

  if cluster_exists "$name"; then
    echo "Cluster ${name} already exists"
    return
  fi

  echo "Creating Kind cluster ${name}"
  kind create cluster --name "$name" --config "$config"
}

load_images() {
  local name="$1"

  for image in "${APP_IMAGES[@]}"; do
    echo "Loading ${image} into ${name}"
    kind load docker-image "$image" --name "$name"
  done
}

deploy_cluster() {
  local name="$1"
  local context="kind-${name}"

  echo "Applying Kubernetes manifests to ${context}"
  kubectl --context "$context" apply -f "$K8S_DIR"

  for deployment in "${DEPLOYMENTS[@]}"; do
    echo "Waiting for ${deployment} in ${context}"
    kubectl --context "$context" rollout status "deployment/${deployment}" --timeout=240s
  done
}

main() {
  require_command docker
  require_command kind
  require_command kubectl
  require_command mvn

  echo "Building jars"
  (cd "$ROOT_DIR" && mvn clean package -DskipTests)

  echo "Building application images"
  docker compose -f "$COMPOSE_FILE" build \
    discovery-server \
    product-service \
    order-service \
    inventory-service \
    user-service

  for cluster in "${CLUSTERS[@]}"; do
    IFS=":" read -r name config <<< "$cluster"
    create_cluster "$name" "$config"
    load_images "$name"
    deploy_cluster "$name"
  done

  cat <<'MSG'

Multi-cluster Kind deployment is ready.

Cluster A:
  kubectl --context kind-mini-commerce-a get pods
  user-service       http://localhost:18081
  product-service    http://localhost:18082
  order-service      http://localhost:18083
  inventory-service  http://localhost:18084
  discovery-server   http://localhost:18761
  zipkin             http://localhost:19411

Cluster B:
  kubectl --context kind-mini-commerce-b get pods
  user-service       http://localhost:28081
  product-service    http://localhost:28082
  order-service      http://localhost:28083
  inventory-service  http://localhost:28084
  discovery-server   http://localhost:28761
  zipkin             http://localhost:29411
MSG
}

main "$@"
