# Twitter Ingestion Server
A daemon service that can ingest real-time tweets from [Twitter Filter Stream API](https://developer.twitter.com/en/docs/tweets/filter-realtime/api-reference/post-statuses-filter) into local gzip files in a daily rotation manner.

## Prerequisite
- Java 8

## Build
```bash
$ cd twittermap
$ sbt 'project datatools' assembly
``` 

## Deploy
Copy the runnable file `datatools/target/scala-2.11/datatools-assembly-1.0-SNAPSHOT.jar` to your server.

## Start service
Run command to start the Twitter Ingestion service:
```bash
java -cp datatools-assembly-1.0-SNAPSHOT.jar edu.uci.ics.cloudberry.datatools.twitter.TwitterIngestionServer
```
