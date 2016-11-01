#!/bin/bash
#to run this script in cron jon hourly, add this line to cron table
#00 * * * * cd /home/kaiyim/cloudberry/; ./webhose.sh
current_time=$(($(date +%s%N)/1000000))
start_time=$((current_time-3600000))
api_key=PLEASE-GIVE-API-KEY
output_path=PLEASH-GIVE-OUTPUT-FILE-PATH

sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.news.WebhoseCollector \
-a $api_key \
-o $output_path \
-t $start_time
"