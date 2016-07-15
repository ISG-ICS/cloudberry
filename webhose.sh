#!/bin/bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.news.WebHostCollector -a PLEASE-GIVE-API-KEY -o OUTPUT-FILE-PATH -t START-TIMESTAMP
"