#!/bin/bash -
#===============================================================================
#
#          FILE: deregisterTwitterMapDataModel.sh
#
#         USAGE: ./script/deregisterTwitterMapDataModel.sh
#
#   DESCRIPTION: CAREFUL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
#                This script DEREGISTERS ds_twitter and population data model from cloudberry
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Shengjie Xu, shengjix@uci.edu
#  ORGANIZATION: ics.uci.edu
#       CREATED: 04/18/2017 21:15:01 PM PDT
#      REVISION:  ---
#===============================================================================

curl -d "@./script/dataModel/deregisterTweets.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/deregister
curl -d "@./script/dataModel/deregisterStatePopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/deregister
curl -d "@./script/dataModel/deregisterCountyPopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/deregister
curl -d "@./script/dataModel/deregisterCityPopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/deregister