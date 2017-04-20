#!/bin/bash -
#===============================================================================
#
#          FILE: twitterMapDeregister.sh
#
#         USAGE: ./script/twitterMapDeregister.sh
#
#   DESCRIPTION: remove twitter map schema from cloudberry
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Hao Chen (), haoc13@uci.edu
#  ORGANIZATION: ics.uci.edu
#       CREATED: 04/19/2017 08:57:01 PM PDT
#      REVISION:  ---
#===============================================================================

curl -d "@./script/twitterMapDeregister.json" -H "Content-Type: application/json" -X POST http://localhost:9000/admin/deregister