#!/bin/bash -
#===============================================================================
#
#          FILE: docker_run_asterix.sh
#
#         USAGE: ./docker_run_asterix.sh tag ncs
#
#   DESCRIPTION: Run Asterix Docker image
#
#       OPTIONS:    tag: master or specific versions, default is master
#                   ncs: number of nc. default is 2
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 10/27/2015 11:06:01 AM PDT
#      REVISION:  ---
#===============================================================================


if [ -z "$1" ]; then
  tag="master"
else
  tag="$1"
fi

if [ -z "$2" ]; then
  ncs=1
else
  ncs=$2
fi

set -o nounset                              # Treat unset variables as an error

echo "stop the existing container"
docker stop cc-${tag}
for ((i=1; $i <= $ncs; i=$i+1 )) do
docker stop "nc-${tag}-${i}"
done

echo "remove the existing container"
docker rm cc-${tag}
for ((i=1; $i <= $ncs; i=$i+1 )) do
docker rm "nc-${tag}-${i}"
done

echo "build the new container"
docker run -d --name=cc-${tag} \
   -p 19000:19000 -p 19001:19001 -p 19002:19002 \
    jianfeng/asterix-cc:${tag} $ncs

ccip=`docker inspect cc-${tag} | grep IPAddress | grep -o '[0-9.]*' | head -1`

sleep 2s
#myhost=`hostname -I | cut -d' ' -f1`
for ((n=1; $n <= $ncs; n=$n+1 ))
do
   docker run -v $PWD/../data:/data -d \
     --name "nc-${tag}-${n}" \
      -p 1000${n}:1000${n} -p 1001${n}:1001${n} \
       -p 500${n}:500${n} -p 501${n}:501${n} \
         -p 502${n}:502${n} -p 503${n}:503${n} \
           jianfeng/asterix-nc:${tag} ${n} $ccip ${ncs}
done

docker logs -f cc-${tag} > cc-${tag}.log 2>&1 &

for ((i=1; $i <= $ncs; i=$i+1 ))
do
  docker logs -f nc-${tag}-${i} > nc-${tag}-${i}.log 2>&1 &
done

sleep 2s
echo "initial the twitter data"
host="http://localhost:19002"

ncip=`docker inspect nc-${tag}-1 | grep IPAddress | grep -o '[0-9.]*' | head -1`
echo "declare data type"
curl -XPOST --data-binary @../aql/ddl.aql $host/aql
echo "upload meta data"
sed "s/\$ncip/$ncip/g" ../aql/update.aql | curl -XPOST --data-binary @- $host/aql

