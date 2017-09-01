#!/bin/bash -
#===============================================================================
#
#          FILE: dockerRunMySqlDB.sh
#
#         USAGE: ./dockerRunMySqlDB.sh
#
#   DESCRIPTION: Run MySQL docker containers
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Haohan Zhang (), hh1680651@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 08/14/2017 23:24:01 PM PDT
#      REVISION:  ---
#===============================================================================

echo "Building the mysql-container ..."
docker rm -f mysql-container
docker rm -f mysql-sample-tweet
sleep 2s
docker run -p 6603:3306 -d --name mysql-container \
    -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
    mysql:latest

echo "Ingesting sample tweets ..."
unzip -o ./script/sample.json.zip -d ./script/

host=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}' mysql-container)

docker run -it --rm --name mysql-sample-tweet \
    --link mysql-container:mysql-container -v "$PWD":/usr/src/myapp \
    -w /usr/src/myapp spittet/php-mysql \
    php ./script/ingestMySqlTwitterToLocalCluster.sh $host
