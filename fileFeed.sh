#!/usr/bin/env bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.FileFeedDriver -src noah/src/main/resources/tw_messages.adm -url 172.17.0.3 -p 10001 -w 500 -b 50 -c 1000"
