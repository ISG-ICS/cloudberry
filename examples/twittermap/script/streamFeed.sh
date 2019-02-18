#!/usr/bin/env bash
#===============================================================================
#
#          FILE: streamFeed.sh
#
#         USAGE: (1) Fill in your own Twitter API access keys and tokens
#                (2) Run the script from parent path
#                ./script/streamFeed.sh
#
#    PARAMETERS: -ck  your consumer key
#                -cs  your consumer secret
#                -tk  your access token
#                -ts  your access secret token
#                -loc  [southwestern corner], [northeastern corner]
#                      (the spatial range to filter the tweets, by default is roughly the U.S.)
#                -u   hostname of AsterixDB
#                -p   port of feed of AsterixDB
#                -tr  keyword1, keyword2, ...
#                     (a list of keywords to filter the tweets, by default is nothing, means all tweets)
#
#===============================================================================
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.feed.TwitterFeedStreamDriver \
-ck
Your Consumer Key
-cs
Your Consumer Secret
-tk
Your Access token
-ts
Your Acces Secret Token
-loc -173.847656,17.644022,-65.390625,70.377854
-u 127.0.0.1 -p 10001 -w 0 -b 50"
