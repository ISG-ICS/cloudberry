#!/usr/bin/env bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.feed.TwitterFeedStreamDriver \
-ck
Your Consumer Key
-cs
Your Consumer Secret
-tk
Your Access token
-ts
Your Acces Secret Token
-loc -173.847656,17.644022,-65.390625,70.377854 \
-u 127.0.0.1 -p 10001 -w 0 -b 50"
