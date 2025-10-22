#!/bin/sh
# Get the namespace from 1st argument. If empty fail

TC=$(ps -ef | grep 8080:8080| grep "n $1"| awk '{print $2}')
AC=$(ps -ef | grep 8030:8080| grep "n $1"| awk '{print $2}')
AM=$(ps -ef | grep 8040:8080| grep "n $1"| awk '{print $2}')
UM=$(ps -ef | grep 8050:8080| grep "n $1"| awk '{print $2}')
IN=$(ps -ef | grep 8090:8080| grep "n $1"| awk '{print $2}')
DB=$(ps -ef | grep 1433:1433| grep "n $1"| awk '{print $2}')

for portforward in $TC $AC $AM $UM; do
  if [ -n "$portforward" ]; then
    echo "Port forward $portforward exists, killing it now"
    kill "$portforward"
  fi
done