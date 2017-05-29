#!/bin/bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.kafka.TweetsProducer \
-ck  your consumer key \
-cs  your consumer secret \
-loc -74.045337,40.691774,-73.837471,40.863796 \
-tk your token key \
-tr trump \
-ts your token secret \
-fo -u 127.0.0.1 -p 10001 -w 0 -b 50 -kaf \
-ks kiwi.ics.uci.edu:9092
-kfktpc TwitterZikaStreaming
-ko"

