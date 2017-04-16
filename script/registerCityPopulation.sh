#!/bin/bash -
#===============================================================================
#
#          FILE: registerCityPopulation.sh
#
#         USAGE: bash ./script/registerCityPopulation.sh
#
#   DESCRIPTION: Insert City population schema into cloudberry
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Shengjie Xu, shengjix@uci.edu
#  ORGANIZATION: ics.uci.edu
#       CREATED: 04/13/2017 15:09:01 PM PDT
#      REVISION:  ---
#===============================================================================

curl -d "@./script/registerCityPopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register