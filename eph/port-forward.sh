#!/bin/sh
# Get the namespace from 1st argument. If empty fail
if [ -z "$1" ]; then
  echo "Namespace is required"
  exit 1
fi

export KUBECONFIG=~/.kube/rndwlt/eph
export http_proxy=http://webproxy.infra.backbase.cloud:8888
export https_proxy=http://webproxy.infra.backbase.cloud:8888

./stop-port-forward.sh

TC=$(ps -ef | grep 8080:8080| grep "n $1"| awk '{print $2}')
AC=$(ps -ef | grep 8030:8080| grep "n $1"| awk '{print $2}')
AM=$(ps -ef | grep 8040:8080| grep "n $1"| awk '{print $2}')
UM=$(ps -ef | grep 8050:8080| grep "n $1"| awk '{print $2}')
IN=$(ps -ef | grep 8090:8080| grep "n $1"| awk '{print $2}')
DB=$(ps -ef | grep 1433:1433| grep "n $1"| awk '{print $2}')

for portforward in $TC $AC $AM $UM $IN $DB; do
  if [ -n "$portforward" ]; then
    echo "Port forward $portforward exists, killing it now"
    kill "$portforward"
  fi
done

kubectl -n $1 port-forward service/token-converter 8080:8080 &
kubectl -n $1 port-forward service/access-control 8030:8080 &
kubectl -n $1 port-forward service/user-manager 8050:8080 &
kubectl -n $1 port-forward service/arrangement-manager 8040:8080 &
kubectl -n $1 port-forward service/investment 8090:8080 &
kubectl -n $1 port-forward dbportforward 1433:1433 &

#kubectl -n $1 port-forward service/mssql-server 1433:1433 &
#kubectl -n $1 port-forward "$(kubectl -n $1 get pods| grep '^limit' -m1 | cut -d' ' -f1)" 18084:8080 &