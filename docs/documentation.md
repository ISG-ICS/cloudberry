---
layout: page
title: Documentation
toc: true
---

## Quick Start

### Prepare the AsterixDB cluster
Cloudberry runs on an Apache AsterixDB cluster. Here are two options to set up the cluster.

* Option 1: Use the prebuilt AsterixDB [docker image](https://hub.docker.com/r/jianfeng/asterixdb/) to run a small cluster on a single machine. 
   - Install [Docker](https://www.docker.com/products/docker)(>1.10) on your local machine
   - Simply run the following command to create a two nc AsterixDB cluster locally. 
   ```
   ./script/dockerRunAsterixDB.sh
   ```

* Option 2: Follow the official [documentation](https://ci.apache.org/projects/asterixdb/install.html) to setup a fully functional cluster.

### Run the TwitterMap demo
Now let's checkout the code and run a TwitterMap demo on your local machine! You will need [`sbt`](http://www.scala-sbt.org/release/docs/Setup.html) to compile the project.

* Clone the code  

```
git clone https://github.com/ISG-ICS/cloudberry.git
```

* Compile the project

```
cd cloudberry; sbt compile
```

* If you set a fully functional cluster through Managix, make the following changes in the script *./script/ingestTwitterToLocalCluster.sh*, line 86:

```
("sockets"="my_asterix_nc1:10001")
```
where *my_asterix* is the name of your cluster instance, and *nc1* is the name of the NC node.

* Ingest the sample data

```
./script/ingestTwitterToLocalCluster.sh
```

* Set the AsterixDB hostname in configuration file `neo/conf/application.conf` locally by changing the `asterixdb.url` value to the previously set AsterixDB address.

```
asterixdb.url = "http://localhost:19002/aql"
```

* Finally run 

```
sbt "project neo" "run"
```

You should see the TwitterMap webpage on your `http://localhost:9000`

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

```
{
  "name":"twitter.ds_tweet",
  "schema":{"typeName":"twitter.typeTweet",
    "dimension":[
      {"name":"create_at","isOptional":false,"datatype":"Time"},
      {"name":"id","isOptional":false,"datatype":"Number"},
      {"name":"coordinate","isOptional":false,"datatype":"Point"},
      {"name":"lang","isOptional":false,"datatype":"String"},
      {"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},
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
      {"name":"favorite_count","isOptional":false,"datatype":"Number"},
      {"name":"retweet_count","isOptional":false,"datatype":"Number"}
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

1. Get the per-state and per-hour count of tweets that contain "zika" and "virus" in 2016.

```
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

2. Get the top-10 related hashtags for tweets that mention "zika"

```
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

3. Get 100 latest sample tweets that mention "zika".

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
}
```

### Request options

Cloudberry supports automatic query-slicing on the `timeField`. The front-end can specify a response time limit for each "small query" to get the results progressively.
For example, the following option specifies that the front-end wants to slice a query and the expected response time for each sliced "small query" is 2000 ms.

```
{
 ...
 "option":{
   "sliceMillis": 2000  
 }
}
```

#### Format of multiple requests
Sometimes the front-end wants to slice multiple queries simultaneously so that it can show multiple consistent results. In this case, it can wrap the queries inside the `batch` field and specify only one `option` field.

```
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
