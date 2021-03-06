#!/bin/bash

deployment_name="${DEPLOYMENT_NAME:-kotlin-vertx-local-environment}"
namespace="${KUBE_NAMESPACE:-kotlin-vertx}"

echo "Shutting down the environment"
helm uninstall "$deployment_name" --namespace="$namespace" || true
