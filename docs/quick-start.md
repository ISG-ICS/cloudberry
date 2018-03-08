---
layout: page
---

## Setup TwitterMap

This page includes instructions on how to use Cloudberry and AsterixDB to setup a small instance of 
[TwitterMap](http://cloudberry.ics.uci.edu/demos/twittermap/) on a local machine.
The following diagram illustrates its architecture: ![architecture][architecture]

System requirements:
 - Linux or Mac
 - At least 4GB memory

## 0. Setup the environment:

**Step 1**: Follow these [instructions](http://www.scala-sbt.org/release/docs/Setup.html) to install `sbt`.

**Step 2**: Clone the Cloudberry codebase from github.

```
~> git clone https://github.com/ISG-ICS/cloudberry.git
```

Suppose the repostory is cloned to the folder `~/cloudberry`.

## 1. Setup AsterixDB

**Step 1**: Create a directory named `asterixdb` in your home directory and move to that directory:
```
$ mkdir asterixdb
$ cd asterixdb
```

**Step 2**: Download `asterix-server-0.9.3-SNAPSHOT-binary-assembly.zip` from this [link](http://cloudberry.ics.uci.edu/img/asterix-server-0.9.3-SNAPSHOT-binary-assembly.zip):

```
$ wget http://cloudberry.ics.uci.edu/img/asterix-server-0.9.3-SNAPSHOT-binary-assembly.zip
```

**Step 3**: Uncompress the file:
```
$ unzip asterix-server-0.9.3-SNAPSHOT-binary-assembly.zip
```

**Step 4**: Move to `opt/local/bin` directory.
```
$ cd opt/local/bin
```

**Step 5**: Execute `start-sample-cluster.sh` to start the sample instance. Wait until you see "INFO: Cluster started and is ACTIVE." message.
```
$ ./start-sample-cluster.sh
CLUSTERDIR=/home/x/asterixdb/opt/local
INSTALLDIR=/home/x/asterixdb
LOGSDIR=/home/x/asterixdb/opt/local/logs

INFO: Starting sample cluster...
INFO: Waiting up to 30 seconds for cluster 127.0.0.1:19002 to be available.
INFO: Cluster started and is ACTIVE.
```


**Step 6**: Execute `jps` to check one instance of "CCDriver" and two instances of "NCService" and "NCDriver" are running:
```
$ jps
59264 NCService
59280 NCDriver
59265 CCDriver
59446 Jps
59263 NCService
59279 NCDriver
```

**Step 7**: Move to `cloudberry/examples/twittermap` directory. Here, suppose you have cloned cloudberry in your home directory.
```
$ cd ~/cloudberry/examples/twittermap
```

**Step 8**: Execute `./script/ingestAllTwitterToLocalCluster.sh` to ingest the sample Tweet data to the AsterixDB instance. Please make sure you already installed "sbt" and "scala". Otherwise, this step will download and install these packages automatically, which will take a lot of time.
```
$ ./script/ingestAllTwitterToLocalCluster.sh
```
This process may take a few minutes depending on your hardware resources.

**Step 9**: Open the AsterixDB Web interface at [http://localhost:19001](http://localhost:19001) and issue the following queries to see the ingestion has finished without an issue.

```
use twitter;
select count(*) from ds_tweet;
select count(*) from dsStatePopulation;
select count(*) from dsCountyPopulation;
select count(*) from dsCityPopulation;

Results:
{ "$1": 47000 }

Results:
{ "$1": 52 }

Results:
{ "$1": 3221 }

Results:
{ "$1": 29833 }
```

Note: You need to execute the following command to stop AsterixDB on `asterixdb/opt/local/bin` before you shutdown the system.
```
$ ./stop-sample-cluster.sh
```

Next time when you want to start/stop your AsterixDB instance, use the following command.
```
$ ~/asterixdb/opt/local/bin/start-sample-cluster.sh
$ ~/asterixdb/opt/local/bin/stop-sample-cluster.sh
```

## 2. Setup Cloudberry and TwitterMap:

**Step 1**: Compile and run the Cloudberry server.

```
~/cloudberry> cd cloudberry
~/cloudberry> sbt compile
~/cloudberry> sbt "project neo" "run"
```

Wait until the shell prints the messages shown in the following screenshot:
![neo][neo]

**Step 2**: Open another terminal window to ingest sample tweets (about 47K) and US population data into AsterixDB.

```
~/cloudberry> cd ../examples/twittermap
~/twittermap> ./script/ingestAllTwitterToLocalCluster.sh
```

When it finishes you should see the messages as shown in the following screenshot:
![ingestion][ingestion]


**Step 2**: Start the TwitterMap Web server (in port 9001) by running the following command in another shell:

```
~/twittermap> sbt "project web" "run 9001"
```

Wait until the shell prints the messages shown in the following screenshot:
![twittermap][twittermap]


**Step 3**: Open a browser to access [http://localhost:9001](http://localhost:9001) to see the TwitterMap frontend.  The first time you open the page, it could take up to several minutes (depending on your machine's speed) to show the following Web page:
![web][web]


**Congratulations!** You have successfully set up TwitterMap using Cloudberry and AsterixDB!

## 3. Under the Hood

Next we explain the details of the TwitterMap.

**3.1** Create a Dataset in AsterixDB

In Step 9 (TODO), we ran a script called `examples/twittermap/script/ingestAllTwitterToLocalCluster.sh` to create data sets in AsterixDB and ingest data into them.
The following are the executed DDL statements.


```
create dataverse twitter if not exists;
use dataverse twitter
create type typeUser if not exists as open {
    id: int64,
    name: string,
    screen_name : string,
    lang : string,
    location: string,
    create_at: date,
    description: string,
    followers_count: int32,
    friends_count: int32,
    statues_count: int64
}
create type typePlace if not exists as open{
    country : string,
    country_code : string,
    full_name : string,
    id : string,
    name : string,
    place_type : string,
    bounding_box : rectangle
}
create type typeGeoTag if not exists as open {
    stateID: int32,
    stateName: string,
    countyID: int32,
    countyName: string,
    cityID: int32?,
    cityName: string?
}
create type typeTweet if not exists as open{
    create_at : datetime,
    id: int64,
    "text": string,
    in_reply_to_status : int64,
    in_reply_to_user : int64,
    favorite_count : int64,
    coordinate: point?,
    retweet_count : int64,
    lang : string,
    is_retweet: boolean,
    hashtags : {{ string }} ?,
    user_mentions : {{ int64 }} ? ,
    user : typeUser,
    place : typePlace?,
    geo_tag: typeGeoTag
}
create dataset ds_tweet(typeTweet) if not exists primary key id
using compaction policy prefix (("max-mergable-component-size"="134217728"),("max-tolerance-component-count"="10")) with filter on create_at ;
create index text_idx if not exists on ds_tweet("text") type fulltext;
```

Read this [page](https://ci.apache.org/projects/asterixdb/sqlpp/primer-sqlpp.html) about the details.

The script uses a feature called `Feed` to ingest tweets into AsterixDB. The following statements create
a feed called `TweetFeed`:

```
create feed TweetFeed using socket_adapter
(
    ("sockets"="$nc:10001"),
    ("address-type"="nc"),
    ("type-name"="typeTweet"),
    ("format"="adm")
);
connect feed TweetFeed to dataset ds_tweet;
start feed TweetFeed;
```

The following shell command ingests the data from a local file with tweets into AsterixDB using the defined `TweetFeed`:


```bash
gunzip -c ./script/sample.adm.gz | ./script/fileFeed.sh $host 10001
```

For more information about AsterixDB data feed, please refer to this [page](https://ci.apache.org/projects/asterixdb/feeds/tutorial.html).

**Step 3.2** Setup Cloudberry

In Step 10, we changed the configuration file of Cloudberry by providing the information about the
AsterixDB instance.

**Step 3.3** Setup TwitterMap Web Server

The TwitterMap Web application uses the [Play Framework](http://playframework.com) to talk to the Cloudberry service. The configuration
of the framework is in the file `examples/twittermap/web/conf/application.conf`.  In the file, the `cloudberry.register` property specifies the
HTTP API of Cloudberry:

```properties
cloudberry.register = "http://CLOUDBERRY-HOST-NAME:CLOUDBERRY-PORT/admin/register"
```

When the TwitterMap server starts, it will run `twittermap/web/app/controllers/TwitterMapApplication.scala`, which will run
`twittermap/web/app/model/Migration_20170428.scala`. This script registers four data sets to the Cloudberry server.
They are:

* twitter.ds_tweet
* twitter.dsStatePopulation
* twitter.dsCountyPopulation
* twitter.dsCityPopulation

Cloudberry will talk to AsterixDB to collect information about these data sets. Take the data set `twitter.ds_tweet` as an example.   The script sends the following DDL request to Cloudberry to register the information about this data set in AsterixDB.

```JSON
{
  "dataset":"twitter.ds_tweet",
  "schema":{
  	"typeName":"twitter.typeTweet",
    "dimension":[
      {"name":"create_at","isOptional":false,"datatype":"Time"},
      {"name":"id","isOptional":false,"datatype":"Number"},
      {"name":"coordinate","isOptional":false,"datatype":"Point"},
      {"name":"lang","isOptional":false,"datatype":"String"},
      {"name":"is_retweet","isOptional":false,"datatype":"Boolean"},
      {"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},
      {"name":"user_mentions","isOptional":true,"datatype":"Bag","innerType":"Number"},
      {"name":"user.id","isOptional":false,"datatype":"Number"},
      {"name":"geo_tag.stateID","isOptional":false,"datatype":"Number"},
      {"name":"geo_tag.countyID","isOptional":false,"datatype":"Number"},
      {"name":"geo_tag.cityID","isOptional":false,"datatype":"Number"},
      {"name":"geo","isOptional":false,"datatype":"Hierarchy","innerType":"Number",
        "levels":[
          {"level":"state","field":"geo_tag.stateID"},
          {"level":"county","field":"geo_tag.countyID"},
          {"level":"city","field":"geo_tag.cityID"}]}
    ],
    "measurement":[
      {"name":"text","isOptional":false,"datatype":"Text"},
      {"name":"in_reply_to_status","isOptional":false,"datatype":"Number"},
      {"name":"in_reply_to_user","isOptional":false,"datatype":"Number"},
      {"name":"favorite_count","isOptional":false,"datatype":"Number"},
      {"name":"retweet_count","isOptional":false,"datatype":"Number"},
      {"name":"user.status_count","isOptional":false,"datatype":"Number"}
    ],
    "primaryKey":["id"],
    "timeField":"create_at"
  }
}
```

In the configuration file `twittermap/web/conf/application.conf`, the `cloudberry.ws` property tells the front-end the Web socket address of the Cloudbery server.

```properties
cloudberry.ws = "ws://CLOUDBERRY_HOST_NAME:CLOUDBERRY_PORT/ws"
```

The frontend uses the web socket to communicate with the Cloudberry server directly. The corresponding logic can be found in `twittermap/web/public/javascripts/common/services.js` file.

For more information about how to write registration DDL and Cloudberry request please refer to this [page](/documentation).

## 4. Build your own application

TwitterMap is one example of how to use Cloudberry. To develop your own application, you can do the following steps:

1. Use AsterixDB to create your own data sets;
2. Give the link of the AsterixDB instance to Cloudberry by following step 10 (TODO);
3. Register the necessary datasets into Cloudberry as in Section 2.3 (TODO);
4. Set up the Web socket connection between the front-end web page and the Cloudberry server as in Section 2.3;
5. Define your queries and responses as in `twittermap/web/public/javascripts/common/services.js`.

Have fun!  If you need assistance, please feel to contact us at
&#105;&#099;&#115;&#045;&#099;&#108;&#111;&#117;&#100;&#098;&#101;&#114;&#114;&#121;&#064;&#117;&#099;&#105;&#046;&#101;&#100;&#117;


[architecture]: /img/twittermap-architecture.png
{: width="800px"}
[docker]: /img/docker.png
{: width="800px"}
[ingestion]: /img/ingestion.png
{: width="800px"}
[neo]: /img/neo.png
{: width="800px"}
[twittermap]: /img/twittermap.png
{: width="800px"}
[web]: /img/web.png
{: width="800px"}
