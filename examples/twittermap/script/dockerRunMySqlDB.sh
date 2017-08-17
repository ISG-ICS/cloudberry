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

set -o nounset                      # Treat unset variables as an error

echo "Starting docker-machine ..."
docker-machine create --driver virtualbox default
docker-machine start
eval $(docker-machine env)

HOST="$(echo -e "$(docker-machine ip)" | sed -e 's/^[[:space:]]*//')"
PORT=3308

echo "Editing config files ..."
sed -i -e 's/# isMySql = true/isMySql = true/g' web/conf/application.conf
sed -i -e 's/asterixdb.lang = SQLPP/##asterixdb.lang = SQLPP/g' ../../cloudberry/neo/conf/application.conf
sed -i -e 's/#asterixdb.lang = SQL/asterixdb.lang = SQL/g' ../../cloudberry/neo/conf/application.conf
sed -i -e 's/localhost:3306/'$HOST':'$PORT'/g' ../../cloudberry/neo/conf/application.conf

echo "Building the mysql-container ..."
docker stop mysql-container
docker rm -f mysql-container
sleep 2s
docker run -p $PORT:3306 -d --name mysql-container \
    -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
    mysql:latest

echo "Ingesting sample tweets ..."
unzip -o ./script/mysqlSample.json.zip -d ./script/
DBNAME=sql
sleep 10s

docker run -it --rm --name mysql-sample-tweet \
    --link mysql-container -v "$PWD":/usr/src/myapp \
    -w /usr/src/myapp spittet/php-mysql \
    php ./script/ingestMySqlTwitterToLocalCluster.sh $PORT $DBNAME
