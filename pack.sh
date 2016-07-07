#!/bin/sh
sbt ";project noah; clean; assembly"
cp noah/target/scala-2.11/noah-assembly-1.0-SNAPSHOT.jar ~/Work/ExternalLib/
zip -rj ~/Work/ExternalLib/geoTag.zip ~/Work/ExternalLib/*
