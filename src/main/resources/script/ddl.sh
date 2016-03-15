#!/usr/bin/env bash

set -o nounset                              # Treat unset variables as an error

tag=$1
echo "initial the twitter data"
host="http://localhost:19002"

ncip=`docker inspect nc-${tag}-1 | grep IPAddress | grep -o '[0-9.]*' | head -1`
echo "declare data type"
curl -XPOST --data-binary @../aql/ddl.aql $host/aql
echo "upload meta data"
sed "s/\$ncip/$ncip/g" ../aql/update.aql | curl -XPOST --data-binary @- $host/aql
