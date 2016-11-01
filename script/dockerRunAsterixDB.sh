#!/bin/bash -
#===============================================================================
#
#          FILE: dockerRunAsterixDB.sh
#
#         USAGE: ./dockerRunAsterixDB.sh ncs
#
#   DESCRIPTION: Run Asterix Docker containers
#
#       OPTIONS:    
#                   ncs: number of nc. 
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 10/27/2015 11:06:01 AM PDT
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

ncs=${1:-2}          # the number of NCs in local cluster, default is 2 ncs
NC_JVM_MEM=2048      # the JVM -Xmx2048m memory budget for each NC. the Unit is in meta bytes

docName=dbstore
docker volume create --driver local --name $docName

echo "build the cc"
docker run -d --name=cc \
   -p 19000:19000 -p 19001:19001 -p 19002:19002 -p 19006:19006 \
    jianfeng/asterixdb cc $ncs

ccip=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' cc)

sleep 2s

for ((n=1; n <= ncs; n=n+1 ))
do
    echo "build nc${n}"
    port=$((10000+n)) # these ports are used for feed 
    docker run -d -v $docName:/db -p $port:$port -e NC_JVM_MEM=$NC_JVM_MEM \
      --name "nc${n}" \
        jianfeng/asterixdb nc ${n} $ccip $ncs
done
