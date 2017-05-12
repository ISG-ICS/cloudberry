---
layout: page
---

## 1. Setup TwitterMap Using Cloudberry and AsterixDB

This page includes instructions on how to use Cloudberry and AsterixDB to setup a small instance of the
[TwitterMap](http://cloudberry.ics.uci.edu/demos/twittermap/) on a local machine. 
The following diagram illustrates its architecture: ![architecture][architecture]

System requirements:

 - Linux or Mac
 - At least 4GB memory

**Step 1**: Follow the instructions on this [`page`](http://www.scala-sbt.org/release/docs/Setup.html) to install `sbt`.

**Step 2**: Clone the Cloudberry codebase from github.

```
~> git clone https://github.com/ISG-ICS/cloudberry.git
```

Suppose the repostory is cloned to the folder `~/cloudberry`.

**Step 3**: Use the following steps to start a Docker container that has a pre-built AsterixDB cluster.

   1. Install [Docker](https://www.docker.com/products/docker) (version at least 1.10) on the local machine;
   2. Run the following commands:

```
~> cd cloudberry
~/cloudberry> ./script/dockerRunAsterixDB.sh
```
The second command will download and run a prebuilt AsterixDB docker container from [here](https://hub.docker.com/r/jianfeng/asterixdb/). This step may take 5-10 minutes or even longer, depending on your network speed.
After it finishes, you should see the messages as shown in the following screenshot:
![docker][docker]

**Step 4**: Run the following command to ingest sample tweets (about 47K) and US population data into AsterixDB.


```
~/cloudberry> ./script/ingestAllTwitterToLocalCluster.sh
```

When it finishes you should see the messages as shown in the following screenshot:
![ingestion][ingestion]

**Step 5**: Compile and run the Cloudberry server.

```
~/cloudberry> sbt compile
~/cloudberry> sbt "project neo" "run"
```

Wait until the shell prints the messages shown in the following screenshot:
![neo][neo]

**Step 6**: Start the TwitterMap Web server (in port 9001) by running the following command in another shell:

```
~/cloudberry> sbt "project twittermap" "run 9001"
```

Wait until the shell prints the messages shown in the following screenshot:
![twittermap][twittermap]


**Step 7**: Open a browser to access [http://localhost:9001](http://localhost:9001) to see the TwitterMap frontend.  The first time you open the page, it could take up to several minutes (depending on your machine's speed) to show the following Web page:
![web][web]


**Congratulations!** You have successfully set up TwitterMap using Cloudberry and AsterixDB!

## Setup your own AsterixDB cluster 

The instructions above assume that we use an AsterixDB instance in a Docker container. If you want to setup your AsterixDB cluster, please use the following steps.

**Step 8**: Follow the instructions on the [AsterixDB Installation Guide](https://ci.apache.org/projects/asterixdb/index.html) to install an AsterixDB cluster.  Select your preferred installation option. 

**Step 9**: Ingest twitter data to AsterixDB

You need to give the RESTFul API link of the AsterixDB cluster and one of its NC names to the ingestion script as following:

```bash
~/cloudberry> ./script/ingestAllTwitterToLocalCluster.sh http://YourAsterixDBServerIP:19002/aql ONE_OF_NC_NAMES
```

**Step 10**: Change the Cloudberry middleware configuration to connect to this new AsterixDB cluster. 
You can modify the AsterixDB hostname in the configuration file `neo/conf/application.conf` by changing the `asterixdb.url` value.

```properties
asterixdb.url = "http://YourAsterixDBHostName:19002/query/service"
```

## 2. Under the Hood

Next we explain the details of the TwitterMap.

### 2.1 Create a Dataset in AsterixDB

In Step 9, we ran a script called `./script/ingestAllTwitterToLocalCluster.sh` to create data sets and ingest data into them. 
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

The following shell command ingests the data from a local file into AsterixDB using the defined `TweetFeed`:


```bash
gunzip -c ./script/sample.adm.gz | ./script/fileFeed.sh $host 10001
```

For more information about AsterixDB data feed, please refer to this [page](https://ci.apache.org/projects/asterixdb/feeds/tutorial.html).

### 2.2 Setup Cloudberry

In Step 10, we changed the configuration file of Cloudberry by providing the information about the
AsterixDB instance.

### 2.3 Setup TwitterMap Web Server

TwitterMap demo is also Play! application that talks with Cloudberry service. Similarity, we can change the
configuration file in `twittermap/conf/application.conf` to provide the information about the Cloudberry service.

The `cloudberry.register` property specify the data registration HTTP API to register the AsterixDB dataset to Cloudberry

```properties
cloudberry.register = "http://CLOUDBERRY-HOST-NAME:CLOUDBERRY-PORT/admin/register"
```

When the TwitterMap server starts, it will register the `twitter.ds_tweet` dataset to the Cloudberry server.
The corresponding logic is implemented in the `twittermap/app/model/Migration_20170428.scala`. Basically, it 
sends the following Cloudberry DDL request to let Cloudberry collect the information of `twitter.ds_tweet` dataset in AsterixDB.

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

The demo also needs to query the population dataset in AsterixDB. The following JSON file declare a  `twitter.dsStatePopulation` dataset.

```JSON
{
    "dataset": "twitter.dsStatePopulation",
    "schema": {
        "typeName": "twitter.typeStatePopulation",
        "dimension": [
            { "name": "name", "isOptional": false, "datatype": "String" },
            { "name": "stateID", "isOptional": false, "datatype": "Number" },
            { "name": "create_at", "isOptional": false, "datatype": "Time" }
        ],
        "measurement": [
            { "name": "population", "isOptional": false, "datatype": "Number" }
        ],
        "primaryKey": ["stateID"],
        "timeField": "create_at"
    }
}
```

The `cloudberry.ws` property is given to the front-end webpage to let the browser directly talk to the Cloudbery server. 

```properties
cloudberry.ws = "ws://CLOUDBERRY_HOST_NAME:CLOUDBERRY_PORT/ws"
```

This web socket is used for send the Cloudberry request and receive the responses. The conresponding logic can be found
in `twittermap/public/javascripts/common/services.js` file.

For more information about how to write registration DDL and Cloudberry request please refer to [documentation](/documentation).

## 3. Build your own application

TwitterMap is one example of how to use Cloudberry. To develop your own application, you will need to change the following places.

1. Give the link of the AsterixDB Instance to Cloudberry by following 2.2
2. Register the necessary datasets into Cloudberry as in 2.3
3. Set up the web socket connection between your front-end webpage and the Cloudberry server as in 2.3
4. Define your query and response handling logic in your own applications. 

Then you should achieve the similar user experience as TwitterMap demo.


[architecture]: /img/quick-start-architecture.png
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



