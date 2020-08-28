#!/bin/bash

if [[ -z "$1" ]]; then
  echo "Version not given.  Usage:"
  echo "$0 <version>"
  exit 1
fi

echo "Will execute:"
echo "mvn org.codehaus.mojo:versions-maven-plugin:2.7:set -DnewVersion=$1 -DprocessAllModules"
echo -n "Continue? [y/N] "
read cont

if [ "y" = "$cont" ] || [ "Y" = "$cont" ]; then
  mvn org.codehaus.mojo:versions-maven-plugin:2.7:set -DnewVersion=$1 -DprocessAllModules
fi
