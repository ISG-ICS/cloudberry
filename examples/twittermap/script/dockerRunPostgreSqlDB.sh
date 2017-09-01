#!/bin/bash -
#===============================================================================
#
#          FILE: dockerRunPostgreSqlDB.sh
#
#         USAGE: ./dockerRunPostgreSqlDB.sh
#
#   DESCRIPTION: Run PostgreSQL docker containers
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Haohan Zhang (), hh1680651@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 08/29/2017 23:24:01 PM PDT
#      REVISION:  ---
#===============================================================================

echo "Building the postgresql-container ..."
docker rm -f postgres-container
docker rm -f postgres-sample-tweet
sleep 2s
docker run --name postgres-container \
    -e POSTGRES_PASSWORD=pwd \
    -d -p 5555:5432 postgres

echo "Ingesting sample tweets ..."
unzip -o ./script/sample.json.zip -d ./script/

host=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}' postgres-container)

docker run -it --rm --name postgres-sample-tweet \
    --link postgres-container:postgres-container -v "$PWD":/usr/src/myapp \
    -w /usr/src/myapp spittet/php-postgresql \
    php ./script/ingestPostgreSqlTwitterToLocalCluster.sh $host
