#Twitter API-> Kafka Cluster -> AsterixDB 

## Build

Prerequisites: scala, sbt, zookeper, apache kafka

### To instal and run automatically Zookeper Server
```
sudo apt-get install zookeeperd
```

### To run kafka server on background
```
nohup ~/kafka/bin/kafka-server-start.sh ~/kafka/config/server.properties > ~/kafka/kafka.log 2>&1 &
```

### Run Twitter Driver and enable kafka producer

```
sbt "project noah" "run-main edu.uci.ics.cloudberry.noah.feed.TwitterFeedStreamDriver \
-ck
consumer key
-cs
consumer secret key
-tk
token
-ts
token secret
-tu
list of keywords separated by comma
-fo
-u
127.0.0.1
-p
10001
-w
0
-b
50
-kaf
-ks
kafka 'hostname:port'  eg.: "localhost:9092"
```

### Get data from Kafka and insert into AsterixDB

*****Run twitter/zika/ddl.aql in Asterix **first**
```
sbt "project noah" "edu.uci.ics.cloudberry.noah.kafka.ConsumerZikaStreaming \
-ks 
kafka 'hostname:port'  eg.: "localhost:9092"
-kid 
an id to identify who is consuming the data eg.: "test1"
-axs
 "server:port" for AsterixDB requests
```

You can replace the `TwitterFeedStreamDriver` to other Twitter Driver on the project, but make sure to also run the related Consumer
