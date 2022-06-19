#!/bin/bash

SCRIPTPATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1; pwd -P)"

deployment_name="${DEPLOYMENT_NAME:-kotlin-vertx}"
namespace="${KUBE_NAMESPACE:-kotlin-vertx}"
timeout="${DEPLOYMENT_TIMEOUT:-3m0s}"

echo "Starting the deployment of the app... ($timeout timeout)"
helm install "$deployment_name" ${SCRIPTPATH}/kotlin-vertx \
    --namespace="$namespace" \
    --wait \
    --timeout "$timeout" $@ || exit 1

echo "Deployment of the app has finished"
kubectl get pods --namespace="$namespace"
