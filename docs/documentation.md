---
layout: page
title: Documentation
toc: true
---
## Quick Start

Now let's checkout the code and run a TwitterMap demo on your local machine!
You will need [`sbt`](http://www.scala-sbt.org/release/docs/Setup.html) to compile the project.
*This demo requires at least 4G memory*.

* Clone the code

```
git clone https://github.com/ISG-ICS/cloudberry.git
```

* Compile the project

```
cd cloudberry; sbt compile
```

* Prepare the AsterixDB cluster: Cloudberry runs on an Apache AsterixDB cluster. You can set up a small AsterixDB cluster locally by using the prebuilt AsterixDB [docker image](https://hub.docker.com/r/jianfeng/asterixdb/).
   1. Install [Docker](https://www.docker.com/products/docker) (>1.10) on your local machine.
   2. Simply run the following command in the **cloudberry** folder to create an AsterixDB cluster locally.

```
./script/dockerRunAsterixDB.sh
```

* Ingest 324,000 sample tweets into AsterixDB

```
./script/ingestTwitterToLocalCluster.sh
```

* Ingest 33,107 US population data into AsterixDB (assume you are using Docker):
* Run the following commands in the **cloudberry** folder to copy three population datasets into docker container `nc1`

```
docker cp noah/src/main/resources/population/adm/allStatePopulation.adm nc1:/home
docker cp noah/src/main/resources/population/adm/allCountyPopulation.adm nc1:/home
docker cp noah/src/main/resources/population/adm/allCityPopulation.adm nc1:/home
```

* Open `noah/src/main/resources/population/sqlpp/ingestPopulation.sqlpp` using a text editor and copy all of its contents. You may need to change the IP address of `nc1` by running the following script:

{% raw %}
```
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' nc1
```
{% endraw %}

* Go to the AsterixDB web interface (`http://localhost:19001/` in the docker case). Copy and paste the `ingestPopulation.sqlpp` content into the web page and execute the query. You should see **Success: Query Complete** in the output.
* Run cloudberry

```
sbt "project neo" "run"
```

*Note when you open the page for the first time, it could take up to several minutes (depending on your machine) to load the front-end data. If you see the following messages from the console, it means the loading process is done.*

```
...
[info] application - I'm initializing
[info] play.api.Play - Application started (Dev)
```

* Once Cloudberry has successfully launched, register data models into Cloudberry by running the following scripts in the **cloudberry** folder

```
./script/registerTwitterMapDataModel.sh
```

* **Congratulations!** You have finished setting up AsterixDB, Cloudberry, and TwitterMap on your localhost. Check it out at [http://localhost:9000](http://localhost:9000) and start playing with it!


## Concepts

The Cloudberry system provides an optimization framework to speed up visualization-oriented OLAP queries on [AsterixDB](http://asterixdb.apache.org).

The following document uses an already ingested AsterixDB Twitter dataset to illustrate how to set up the Cloudberry system on the dataset.

```
create type typeUser if not exists as open {
   id: int64,
   name: string,
   screen_name : string,
   lang : string
}

create type typeGeoTag if not exists as open {
   stateID: int32,
   countyID: int32,
   cityID: int32?
}

create type typeTweet if not exists as open{
   create_at : datetime,
   id: int64,
   coordinate: point?,
   "text": string,
   favorite_count : int64,
   retweet_count : int64,
   lang : string,
   hashtags : { {string } } ?,
   user_mentions : { { int64 } } ? ,
   user : typeUser,
   geo_tag: typeGeoTag
}
```

## Data Schema

Front-end developers need to tell Cloudberry which dataset to query and how the dataset looks like so that it can utilize the Cloudberry optimization techniques.

The data set schema declaration is composed of five distinct components.

* **Dataset name and its type** : the data set name to access AsterixDB.
* **Dimensions** : the columns to do "group by" on. They are usually the x-axis in a visualization figure.
* **Measurements** : the columns to apply the aggregation functions on, such as `count()`, `min()`, `max()`. They can also be used to filter the data but they should not be used as "group by" keys.
* **Primary key** : the primary column name.
* **Time field** : the time column name.

The following JSON request can be used to register the Twitter dataset inside AsterixDB to the middleware.

```json
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

*Note*:
Fields that are not relevant to the visualization queries are not required to appear in the schema declaration.

### Data Types

Cloudberry supports the following data types:

* **Boolean** : the same `Boolean` type as in AsterixDB.
* **Number** : a superset to include all `int8`, `int32`, `int64`, `float`, `double` datatypes in AsterixDB.
* **Point** : the same `point` type as in AsterixDB. Currently, we only support geo-location points.
* **Time** : the same `datetime` type as in AsterixDB.
* **String** : same as the `string` type in AsterixDB. It usually is an identity name which is used to do filtering and "group by".
* **Text** : it is the `string` type as in AsterixDB. The attribute has to be a `measurement` and can only be used to do filtering by a full-text search. Usually, it implies there is an inverted-index built on the field.
* **Bag** : the same `set` type as in AsterixDB.
* **Hierarchy** : A synthetic field that defines hierarchical relationships between the existing fields.

#### Pre-defined functions

| Datatype | Filter | Groupby | Aggregation |
|:--------|:-------:|:--------:|--------:|
|Boolean | isTrue, isFalse | self | distinct-count |
|Number  | <, >, ==, in, inRange | bin(scale) | count, sum, min, max, avg |
|Point   | inRange   | cell(scale) | count  |
|Time    | <, >, ==, inRange | interval(x hour) | count |
|String  | contains, matchs, ~= | self | distinct-count, topK |
|Text    | contains  |  |  distinct-count, topK (on word-token result) |
|Bag     | contains  |  |  distinct-count, topK (on internal data) |
|Hierarchy |         | rollup | |


## Format of requests to the middleware

After defining the dataset, the front-end can send a JSON request to query it.
A request is composed of the following parameters:

* **Dataset** : the dataset to query on.
* **Unnest** : to flatten a record based on the nested `Bag` attribute to generate multiple records.
* **Filter** : a set of selection predicates.
* **Group** :
  * **by** : to specify the "group by" fields.
  * **aggregate**: to specify the aggregation functions to apply, such as `min` and `max`.
* **Select**: to provide *order* or *project* options. It should be mainly used for sampling purposes. A `limit` field should be given. A `offset` field enables pagination.

### Examples

* Get the per-state and per-hour count of tweets that contain "zika" and "virus" in 2016.

```json
{
  "dataset": "twitter.ds_tweet",
  "filter": [
    {
      "field": "create_at",
      "relation": "inRange",
      "values": [ "2016-01-01T00:00:00.000Z", "2016-12-31T00:00:00.000Z"]
    },
    {
      "field": "text",
      "relation": "contains",
      "values": [ "zika", "virus" ]
    }
  ],
  "group": {
    "by": [
        {
          "field": "geo.state",
          "as": "state"
        },
        {
          "field": "create_at",
          "apply": {
            "name": "interval",
            "args": {
              "unit": "hour"
            }
          },
          "as": "hour"
        }
      ],
    "aggregate": [
      {
        "field": "*",
        "apply": {
          "name": "count"
        },
        "as": "count"
      }
      ]
  }
}
```

* Get the top-10 related hashtags for tweets that mention "zika".

```json
{
  "dataset": "twitter.ds_tweet",
  "filter": [
  {
    "field": "text",
    "relation": "contains",
    "values": [ "zika"]
  }
  ],
  "unnest" : [{ "hashtags": "tag"}],
  "group": {
    "by": [
      { "field": "tag" }
    ],
    "aggregate": [
      {
        "field" : "*",
        "apply" : {
          "name": "count"
        },
        "as" : "count"
      }
    ]
  },
  "select" : {
    "order" : [ "-count"],
    "limit": 10,
    "offset" : 0
  }
}
```

* Get 100 latest sample tweets that mention "zika".

```json
{
  "dataset": "twitter.ds_tweet",
  "filter": [{
    "field": "text",
    "relation": "contains",
    "values": [ "zika"]
  }],
  "select" : {
    "order" : [ "-create_at"],
    "limit": 100,
    "offset" : 0,
    "field": ["create_at", "id"]
  }
}
```

### Request options

Cloudberry supports automatic query-slicing on the `timeField`. The front-end can specify a response time limit for each "small query" to get the results progressively.
For example, the following option specifies that the front-end wants to slice a query and the expected response time for each sliced "small query" is 2000 ms.

```json
{
 ...
 "option":{
   "sliceMillis": 2000  
 }
}
```

#### Format of multiple requests

Sometimes the front-end wants to slice multiple queries simultaneously so that it can show multiple consistent results. In this case, it can wrap the queries inside the `batch` field and specify only one `option` field.

```json
{
  "batch" : [
    { request1 },
    { request2 }
  ],
  "option" : {
    "sliceMillis": 2000  
  }
}
```

#### Transform response format

The front end can **optionally** add a "transform" operation in JSON request to define the post-processing operations. 
For example, front-ends can define a `wrap` operation to wrap the whole response in a key-value pair JSON object in which the `key` is pre-defined. The following request asks the Cloudberry to wrap the result in the value with the key of `sample`:

```
{
  "dataset": "twitter.ds_tweet",
  "filter": [{
    "field": "text",
    "relation": "contains",
    "values": [ "zika"]
  }],
  "select" : {
    "order" : [ "-create_at"],
    "limit": 100,
    "offset" : 0,
    "field": ["create_at", "id"]
  }
  "transform" : {
    "warp": {
      "key": "sample"
    }
  }
}
```

The response is as below:

```json
{
  "key":"sample",
  "value":[[
    {"create_at":"2016-10-04T10:00:17.000Z","id":783351045829357568,"user.id":439304013},
    {"create_at":"2016-09-09T10:00:28.000Z","id":774291393749643264,"user.id":2870762297},
    {"create_at":"2016-09-09T10:00:08.000Z","id":774291307858722820,"user.id":2870783428},
    {"create_at":"2016-09-07T10:00:15.000Z","id":773566563895042049,"user.id":2870762297},
    {"create_at":"2016-09-06T10:39:19.000Z","id":773214008237318144,"user.id":3783815248},
    {"create_at":"2016-08-31T10:00:24.000Z","id":771029887025090560,"user.id":2866011003},
    {"create_at":"2016-08-25T10:00:07.000Z","id":768855489073455104,"user.id":115230811},
    {"create_at":"2016-08-19T10:00:36.000Z","id":766681282453594112,"user.id":254059750},
    {"create_at":"2016-08-09T10:00:35.000Z","id":763057397464043521,"user.id":383512034},
    {"create_at":"2016-08-08T10:00:29.000Z","id":762694985355436032,"user.id":518129224}
  ]]
}
```

`wrap` transformation is often preferable when the front-end send many different requests in the same WebSocket interface. 

### Advanced users

#### Multi-node AsterixDB cluster

Some applications may require a multi-node AsterixDB cluster.
You can follow the official [documentation](https://ci.apache.org/projects/asterixdb/install.html) to set it up.

After the cluster is set up, you should make the following changes

* Change the AsterixDB NC name for feed connection

In the script `./script/ingestTwitterToLocalCluster.sh`, line 86:

```
("sockets"="my_asterix_nc1:10001")
```

where *my_asterix* is the name of your cluster instance, and *nc1* is the name of one NC node.


* Modify the AsterixDB hostname

In configuration file `neo/conf/application.conf`, chang the `asterixdb.url` value to the previously set AsterixDB CC RESTFul API address.

```
asterixdb.url = "http://YourAsterixDBHostName:19002/query/service"
```

#### Deregister Tweets and US Population Data Models

You may also deregister these data models from the Cloudberry to experiment with other data models. But be careful when doing this as the TwitterMap won't work without these data models.

```
./script/deregisterTwitterMapDataModel.sh
```
