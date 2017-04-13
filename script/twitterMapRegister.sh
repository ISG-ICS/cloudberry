#!/bin/bash -
#===============================================================================
#
#          FILE: twitterMapRegister.sh
#
#         USAGE: ./twitterMapRegister.sh
#
#   DESCRIPTION: Insert twitter map schema to cloudberry
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Hao Chen (), haoc13@uci.edu
#  ORGANIZATION: ics.uci.edu
#       CREATED: 04/12/2017 01:01:01 PM PDT
#      REVISION:  ---
#===============================================================================

curl -d "@./script/twitterMapRegister.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/register