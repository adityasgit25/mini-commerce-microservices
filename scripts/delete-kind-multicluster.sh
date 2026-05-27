#!/usr/bin/env bash
set -euo pipefail

for cluster in mini-commerce-a mini-commerce-b; do
  if kind get clusters | grep -qx "$cluster"; then
    echo "Deleting Kind cluster ${cluster}"
    kind delete cluster --name "$cluster"
  else
    echo "Cluster ${cluster} does not exist"
  fi
done
