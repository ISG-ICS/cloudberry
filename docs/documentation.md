---
layout: page
title: Documentation
toc: true
---

## Quick Start

### Prepare the AsterixDB cluster
Cloudberry relies on the AsterixDB service, so you will need to set up it first.

* Option 1: Follow the official [documentation](https://ci.apache.org/projects/asterixdb/install.html) to setup a fully functional cluster.
* Option 2: Use the prebuilt AsterixDB docker image to run a small cluster locally. This approach serves the debug purpose.
You can use [our script](https://github.com/ISG-ICS/cloudberry/blob/master/script/dockerRunAsterixDB.sh) to set up a two node cluster.

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

* Ingest the sample data

```
./script/ingestTwitterToLocalCluster.sh
```

* Set the AsterixDB hostname in configuration file `neo/conf/application.conf` locally by changing the `asterixdb.url` value to the previous set AsterixDB address.

```
asterixdb.url = "http://localhost:19002/aql"
```

* Finally run `sbt "project neo" "run"`, you should see the TwitterMap webpage on your `http://localhost:9000`

## Concepts
The Cloudberry system provides an optimization framework to speed up the visualization-oriented OLAP queries against [AsterixDB](http://asterixdb.apache.org) datasets. The Data is stored inside AsterixDB. Users need to take care of the data loading (or ingestion) process.

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
Front-end developers need to tell Cloudberry which dataset to query on and how the dataset looks like so that it can utilize the Cloudberry optimization logics.

**TODO**

  >  Currently it is hardcoded by sending a message. Will make a RESTFul API for register a dataset. To be fixed soon.

The data set schema declaration is composed of five distinct components.

* **Dataset name and its type** : the data set name to access AsterixDB.
* **Dimensions** : dimensions are the columns to group on. It usually is the axis of the figure inside the visualization systems.
* **Measurements** : measurements are the columns to apply the aggregation functions on, such as `count()`, `min()`, `max()`. It can also be used to filter the data but it should not be used as the group keys.
* **Primary key** : the primary column name.
* **Time field** : the time column name.
* [Optional] *Query* : developers can also pre-define a materialized view explicitly in order to speed up the future query. Cloudberry will take care of the maintenance of the view.

The following JSON request can be used to register the Twitter dataset in AsterixDB to Cloudberry system.

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

### Data Type
The system has the following data types

* **Boolean** : the same type as AsterixDB
* **Number** : a superset to include all `int8`, `int32`, `int64`, `float`, `double` in AsterixDB.
* **Point** : same as `point` type in AsterixDB. However, currently, we only support geo-location points.
* **Time** : the `datetime` field in AsterixDB.
* **String** : same as the `string` type in AsterixDB. It usually is an identity name which is used to filter and group on.
* **Text** : it should be the `string` type in AsterixDB. Different from the `String` type, it is used to check its internal contents. Thus, the attribute can only be the `measurement` and can only be used to filter by the full-text search. Usually, it implies there is an inverted-index built on the field.
* **Bag** : A bag of types that contains the same amount of data. Need to declare the `innerType`.
* **Hierarchy** : A synthetic field that tells the hierarchy relationships between the existing fields.

#### Pre-define functions

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


## Request
After defining the dataset, the front-end can send the JSON request to query it.
A request is composed of the following parts.

* **Dataset** : specify which dataset to query on.
* **Lookup** : **TODO**
* **Unnest** : flatten a record based on the nested `Bag` attribute to generate the multiple records.
* **Filter** : filter the dataset
* **Group** : it contains `by` and `aggregate` two parts.
  * **by** : specify which fields to group on.
  * **aggregate**: specify the aggregation functions to apply.
* **Select**: provide *order* or *project* options. It should be mainly used for the sampling purpose. The `limit` field should be given. The `offset` field enables a pagination if the user wants more.

### Examples

1. Get the per-state and per-hour count of tweets that contains "zika" and "virus" in 2016.

```
{
  "dataset": "twitter.ds_tweet",
  "filter": [
    {
      "field": "create_at",
      "relation": "inRange",
      "values": [ "2016-01-01T00:00:00.000Z", "2016-12-01T00:00:00.000Z"]
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

#### Execution options
The Cloudberry supports the automatic query-slicing on the `timeField`. The front-end can specify the slicing response time requirement to get the progressive data.

E.g., the following option specify that the client accepts the sliced response and the expected return time is 2000 ms.

```
{
 ...
 "option":{
   "sliceMillis": 2000  
 }
}
```

#### Multiple requests
Sometimes the front-end wants to slice multiple queries simultaneously so that it can show the consistent multiple results. In that case, it can wrap the queries inside the `batch` fields and specifies only one `option` fields.

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
