#!/usr/bin/env bash
sbt "project noah" 'run-main edu.uci.ics.cloudberry.noah.feed.TwitterFeedUsersTimelineDriver \
-ck Your Consumer Key \
-cs Your secret consumer key \
-tk Your token \
-ts Your secret token \
-tu "eZikaVirus_com"'
