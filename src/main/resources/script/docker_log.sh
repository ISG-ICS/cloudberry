#!/usr/bin/env bash

tag=$1
ncs=$2

docker logs -f cc-${tag} > cc-${tag}.log 2>&1 &

for ((i=1; $i <= $ncs; i=$i+1 ))
do
  docker logs -f nc-${tag}-${i} > nc-${tag}-${i}.log 2>&1 &
done
