#!/bin/bash -
#===============================================================================
#
#          FILE: registerTwitterMapDataModel.sh
#
#         USAGE: ./script/registerTwitterMapDataModel.sh
#
#   DESCRIPTION: Register ds_twitter and population data model into cloudberry
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

curl -d "@./script/dataModel/registerTweets.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register
curl -d "@./script/dataModel/registerStatePopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register
curl -d "@./script/dataModel/registerCountyPopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register
curl -d "@./script/dataModel/registerCityPopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register