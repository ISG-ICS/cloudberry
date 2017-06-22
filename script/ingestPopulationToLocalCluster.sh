#!/bin/bash -
#===============================================================================
#
#          FILE: ingestPopulationToLocalCluster.sh
#
#         USAGE: ./ingestPopulationToLocalCluster.sh
#
#   DESCRIPTION: Ingest the population data to AsterixDB (this one must be executed after ingestTwitterToLocalCluster.sh)
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Chen Luo (), cluo8@uci.edu
#  ORGANIZATION: ics.uci.edu
#       CREATED: 04/24/2017 20:49:01 PM PDT
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

host=${1:-'http://localhost:19002/aql'}
nc=${2:-"nc1"}
# ddl to register the twitter dataset
cat <<EOF | curl -XPOST --data-binary @- $host
use dataverse twitter;
create type typeStatePopulation if not exists as open{
    name:string,
    population:int64,
    stateID:int64
}
create dataset dsStatePopulation(typeStatePopulation) if not exists primary key stateID;

create type typeCountyPopulation if not exists as open{
    name:string,
    population:int64,
    countyID:int64,
    stateName:string,
    stateID:int64
}
create dataset dsCountyPopulation(typeCountyPopulation) if not exists primary key countyID;

create type typeCityPopulation if not exists as open{
    name:string,
    population:int64,
    cityID:int64,
    countyName:string,
    countyID:int64,
    stateName:string,
    stateID:int64
}
create dataset dsCityPopulation(typeCityPopulation) if not exists primary key cityID;

create feed StatePopulationFeed using socket_adapter
(
    ("sockets"="$nc:10002"),
    ("address-type"="nc"),
    ("type-name"="typeStatePopulation"),
    ("format"="adm")
);
connect feed StatePopulationFeed to dataset dsStatePopulation;
start feed StatePopulationFeed;

create feed CountyPopulationFeed using socket_adapter
(
    ("sockets"="$nc:10003"),
    ("address-type"="nc"),
    ("type-name"="typeCountyPopulation"),
    ("format"="adm")
);
connect feed CountyPopulationFeed to dataset dsCountyPopulation;
start feed CountyPopulationFeed;

create feed CityPopulationFeed using socket_adapter
(
    ("sockets"="$nc:10004"),
    ("address-type"="nc"),
    ("type-name"="typeCityPopulation"),
    ("format"="adm")
);
connect feed CityPopulationFeed to dataset dsCityPopulation;
start feed CityPopulationFeed;
EOF

echo 'Created population datasets in AsterixDB.'
#Serve socket feed using local file
cat ./noah/src/main/resources/population/adm/allStatePopulation.adm | ./script/fileFeed.sh $host 10002
echo 'Ingested state population dataset.'

cat ./noah/src/main/resources/population/adm/allCountyPopulation.adm | ./script/fileFeed.sh $host 10003
echo 'Ingested county population dataset.'

cat ./noah/src/main/resources/population/adm/allCityPopulation.adm | ./script/fileFeed.sh $host 10004
echo 'Ingested city population dataset.'

cat <<'EOF' | curl -XPOST --data-binary @- $host
use dataverse twitter;
stop feed CityPopulationFeed;
drop feed CityPopulationFeed;
stop feed CountyPopulationFeed;
drop feed CountyPopulationFeed;
stop feed StatePopulationFeed;
drop feed StatePopulationFeed;
EOF