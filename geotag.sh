#!/bin/bash -
#===============================================================================
#
#          FILE: geotag.sh
#
#         USAGE: ./geotag.sh < read stdin > write stdout
#
#   DESCRIPTION:
#
#       OPTIONS: ---
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 04/17/2016 01:06:30 PM PDT
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

sbt --error 'set showSuccess := false' "project noah" "run -state neo/public/data/state.shape.20m.json\
    -county neo/public/data/county.shape.20m.json \
    -city neo/public/data/cities \
    -helpstate neo/public/data/hierarchy/state.json \
    -helpcounty neo/public/data/hierarchy/county.json \
    -helpcity neo/public/data/hierarchy/cities.json"
