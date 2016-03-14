#!/bin/bash -

set -o nounset                              # Treat unset variables as an error

cmd=$1
tag=$2
ncs=$3

docker $cmd cc-${tag}
for ((i=1; $i <= $ncs; i=$i+1 )) do
docker $cmd "nc-${tag}-${i}"
done

