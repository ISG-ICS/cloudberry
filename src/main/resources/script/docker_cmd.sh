#!/bin/bash -

set -o nounset                              # Treat unset variables as an error

tag=$1
ncs=$2
cmd=$3

docker $cmd cc-${tag}
for ((i=1; $i <= $ncs; i=$i+1 )) do
docker $cmd "nc-${tag}-${i}"
done

