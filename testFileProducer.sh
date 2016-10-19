#!/usr/bin/env bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.kafka.FileProducer \
-kaf \
-ks localhost:9092 \
-fp ./noah/src/main/resources \
-kfktpc TwitterZikaStreaming"
