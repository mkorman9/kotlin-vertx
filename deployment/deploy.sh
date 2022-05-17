#!/bin/bash

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

deployment_name="${DEPLOYMENT_NAME:-kotlin-vertx}"
namespace="${KUBE_NAMESPACE:-kotlin-vertx}"
timeout="${DEPLOYMENT_TIMEOUT:-3m0s}"

if helm get all "$deployment_name" --namespace="$namespace" &> /dev/null; 
then
    echo "Starting the upgrade of the app... ($timeout timeout)"
    helm upgrade "$deployment_name" ${SCRIPTPATH}/kotlin-vertx \
        --namespace="$namespace" \
        --wait \
        --timeout "$timeout" $@ || exit 1
else
    echo "Starting the deployment of the app... ($timeout timeout)"
    helm install "$deployment_name" ${SCRIPTPATH}/kotlin-vertx \
        --namespace="$namespace" \
        --wait \
        --timeout "$timeout" $@ || exit 1
fi

echo "Deployment of the app has finished"
kubectl get pods --namespace="$namespace"
