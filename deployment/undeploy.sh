#!/bin/bash

deployment_name="${DEPLOYMENT_NAME:-kotlin-vertx}"
namespace="${KUBE_NAMESPACE:-kotlin-vertx}"

echo "Shutting down the deployment of the app"
helm uninstall "$deployment_name" --namespace="$namespace"
