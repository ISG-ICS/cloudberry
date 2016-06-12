#!/usr/bin/env bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.feed.FileFeedDriver \
noah/src/main/resources/tw_messages.adm \
-u kiwi.ics.uci.edu -p 10001 -w 500 -b 50 -c 1000"
