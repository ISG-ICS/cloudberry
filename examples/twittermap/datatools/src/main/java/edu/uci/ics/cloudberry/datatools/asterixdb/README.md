# AsterixDB Ingestion Driver
A Java program to geotag and ingest Twitter JSON data into AsterixDB Socket Feed.
 - mode (1) - geotag and ingest tweets from Twitter Ingestion Server Proxy.
 - mode (2) - ingest geotagged tweets from stdin.

## Prerequisite
- Java 8

## Build
```bash
$ cd twittermap
$ sbt 'project datatools' assembly
``` 

## Deploy
Copy the runnable file `datatools/target/scala-2.11/datatools-assembly-1.0-SNAPSHOT.jar` to your server.

## Make a shell script for mode (1)
Create a `ingestProxy.sh` file with content:
```bash
#!/usr/bin/env bash                                                                                                                                                                                    
java -cp /path/to/datatools-assembly-1.0-SNAPSHOT.jar \
edu.uci.ics.cloudberry.datatools.asterixdb.AsterixDBIngestionDriver \
    -fp ws://localhost:9088/proxy \
    -state /path/to/state.json \
    -county /path/to/county.json \
    -city /path/to/city.json \
    -h localhost \
    -p 10001
```
##### Note: The json files of state, county and city can be found in `twittermap/web/public/data/`.

## Make a shell script for mode (2)
Create a `ingestFile.sh` file with content:
```bash
#!/usr/bin/env bash                                                                                                                                                                                    
java -cp /path/to/datatools-assembly-1.0-SNAPSHOT.jar \
edu.uci.ics.cloudberry.datatools.asterixdb.AsterixDBIngestionDriver \
    -h localhost \
    -p 10001
```

## Run AsterixDBIngestionDriver to geotag and ingest Twitter data from proxy server.
```bash
./ingestProxy.sh
```

## Run AsterixDBIngestionDriver to ingest geotagged tweets from stdin.
Suppose your Twitter JSON data is in file `Tweet_2020-05-03.gz`.
```bash
gunzip -c Tweet_2020-05-03.gz | ./geotag.sh 2> /dev/null | grep '^{' | ./ingestFile.sh
```