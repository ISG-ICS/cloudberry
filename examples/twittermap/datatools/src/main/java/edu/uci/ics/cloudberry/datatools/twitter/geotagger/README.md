# Twitter GeoTagger
A Java program to geoTag Twitter JSON with {stateName, stateID, countyName, countyID, cityName, cityID}.

## Prerequisite
- Java 8

## Build
```bash
$ cd twittermap
$ sbt 'project datatools' assembly
``` 

## Deploy
Copy the runnable file `datatools/target/scala-2.11/datatools-assembly-1.0-SNAPSHOT.jar` to your server.

## Make a shell script
Create a `geotag.sh` file with content:
```bash
#!/usr/bin/env bash                                                                                                                                                                                    
java -cp /path/to/datatools-assembly-1.0-SNAPSHOT.jar \
edu.uci.ics.cloudberry.datatools.twitter.geotagger.TwitterGeoTagger \
    -state /path/to/state.json \
    -county /path/to/county.json \
    -city /path/to/city.json \
    -thread 4
```
##### Note: The json files of state, county and city can be found in `twittermap/web/public/data/`.

## Run TwitterGeoTagger against Twitter JSON data
Suppose your Twitter JSON data is in file `Tweet_2020-05-03.gz`.
```bash
gunzip -c Tweet_2020-05-03.gz | ./geotag.sh > Tweet_2020-05-03_geotagged.json
```