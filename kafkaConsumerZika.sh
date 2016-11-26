#!/usr/bin/env bash
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.kafka.ConsumerZikaStreaming \
-ks localhost:9092 \
-kid test1 \
-u 127.0.0.1 -p 10001
-ko"