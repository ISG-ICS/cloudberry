# Berry Guardian
A configurable monitor service to check the status of each service in the Cloudberry and Twittermap stack in a heart beat manner.

## Build
```bash
$ cd twittermap
$ sbt 'project guardian' assembly
``` 

## Config
Modify the `guardian.yaml` file to match your server configurations.

## Deploy
Copy the runnable file `guardian/target/scala-2.11/guardian-assembly-1.0-SNAPSHOT.jar` and config file `guradian.yaml` to your server.

## Start service
Run command to start the guardian service:
```bash
java -cp guardian-assembly-1.0-SNAPSHOT.jar edu.uci.ics.cloudberry.guardian.Guardian -c guardian.yaml
```
