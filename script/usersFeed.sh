#!/usr/bin/env bash
sbt "project noah" 'run-main edu.uci.ics.cloudberry.noah.feed.TwitterUserStreamDriver \
-ck Your Consumer Key \
-cs Your secret consumer key \
-tk Your token \
-ts Your secret token \
-tu "Halt_Zika"
"zikadatabase"
"EbolaRR"
"ZikaECN"
"PRcontraelZIKA"
"Tropicolog"
"zikavirusnet"
"NewsZika"
"zika_updates"
"ZikaVirusTopNws"
"zikavirusmap"
"VirusZikaInfo"
"Zika_Virus_info"
"_zikavirus"
"El_Virus_Zika"
"SahmedGun"
"eZikaVirus_com"
"Zika_Alerts"
"Zika_News"'
