#!/bin/bash

SCRIPTPATH="$(cd -- "$(dirname "$0")" >/dev/null 2>&1; pwd -P)"

$SCRIPTPATH/down.sh &> /dev/null

deployment_name="${DEPLOYMENT_NAME:-kotlin-vertx-local-environment}"
namespace="${KUBE_NAMESPACE:-kotlin-vertx}"
timeout="${DEPLOYMENT_TIMEOUT:-3m0s}"

echo "Starting the environment deployment... ($timeout timeout)"

helm install "$deployment_name" ${SCRIPTPATH} \
  --namespace="$namespace" \
  --wait \
  --wait-for-jobs \
  --timeout "$timeout" $@ || exit 1

echo "Environment deployment has finished"
kubectl get pods --namespace="$namespace"
