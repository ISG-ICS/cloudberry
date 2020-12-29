#!/bin/bash -
#===============================================================================
#
#          FILE: elasticGeoTag.sh
#
#         USAGE: ./elasticGeoTag.sh < read stdin > write stdout
#
#   DESCRIPTION:
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Dayue Bai (dayueb@uci.edu), Baihao Wang (baihaow@uci.edu)
#  ORGANIZATION: ics.uci.edu
#       CREATED: 11/02/2019 21:29:00 PM PST
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

thread=${1:-1}
sbt -mem 2048 "project noah" --error 'set showSuccess := false'  "run-main edu.uci.ics.cloudberry.noah.TwitterJSONTagToADM\
    -state web/public/data/state.json\
    -county web/public/data/county.json \
    -city web/public/data/city.json \
    -thread $thread \
    -fileFormat \"JSON\""
