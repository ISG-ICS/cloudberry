#!/usr/bin/env bash
sbt "project noah" --error "run-main edu.uci.ics.cloudberry.noah.feed.FileFeedDriver \
-u localhost -p 10001 "
