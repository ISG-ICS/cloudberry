#!/bin/bash -
#===============================================================================
#
#          FILE: registerStatePopulation.sh
#
#         USAGE: bash ./script/registerStatePopulation.sh
#
#   DESCRIPTION: Insert state population schema into cloudberry
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

curl -d "@./script/registerStatePopulation.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register