#!/bin/bash -
#===============================================================================
#
#          FILE: ingestAllTwitterToLocalCluster.sh
#
#         USAGE: ./ingestAllTwitterToLocalCluster.sh
#
#   DESCRIPTION: Ingest the twitter data and population data to AsterixDB 
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

host=${1:-'http://localhost:19002/aql'}
echo "Ingesting sample tweets..."
./script/ingestTwitterToLocalCluster.sh $host

echo "Ingesting population data..."
./script/ingestPopulationToLocalCluster.sh $host

echo "Data ingestion completed!"