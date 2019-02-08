#!/bin/sh
#===============================================================================
#
#          FILE: OracleTwittermapIngestion.sh
#
#         USAGE: OracleTwittermapIngestion.sh
#
#   DESCRIPTION: Creates and sql file to ingest 4k twittermap data and ingestion
#                of population data 
#       OPTIONS:
#  REQUIREMENTS: Python 3.7, cx_Oracle
#          BUGS: ---
#         NOTES: install cx_Oracle on server with "pip3 install cx_Oracle --updrade --user"
#        AUTHOR: Tao Wang (), taow8@uci.edu
#  ORGANIZATION: ics.uci.edu
#       CREATED: 12/06/2018 10:35:01 AM PDT
#      REVISION:  ---
#===============================================================================

unzip ./script/sample.json.zip

python3 ./script/ingestOracledata.py

python3 ./script/ingestpopulationOracle.py