#!/usr/bin/env bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.feed.TwitterFeedStreamDriver \
-ck Your customer key \
-cs Your customer secret \
-loc -124.871654,32.918898,-63.627129,49.396314 \
-tk Your token \
-tr trump \
-ts Your token secret \
-u 127.0.0.1 -p 10001 -w 0 -b 50"
