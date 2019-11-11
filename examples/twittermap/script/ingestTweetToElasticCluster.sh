#!/bin/bash -
#===============================================================================
#
#          FILE: ingestTweetToElasticCluster.sh
#
#   DESCRIPTION: Ingest the twitter data data to Elasticsearch cluster
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Dayue Bai (dayueb@uci.edu), Baihao Wang (baihaow@uci.edu)
#  ORGANIZATION: ics.uci.edu
#       CREATED: 11/02/2019 21:29:00 PM PST
#      REVISION:  ---
#===============================================================================

set -o nounset # Treat unset variables as an error

printf "[info] Creating an index named twitter.ds_tweet with the following schema in Elasticsearch...\n\n"
curl -X PUT "localhost:9200/twitter.ds_tweet" -H 'Content-Type: application/json' -d'
{
    "mappings" : {
        "_doc" : {
            "properties" : {
                "create_at" : {"type": "date", "format": "strict_date_time"},
                "text": {"type": "text", "fields": {"keyword": {"type": "keyword","ignore_above": 256}}},
                "id": {"type" : "long"},
                "hashtags": {"type": "text", "fields": {"keyword": {"type": "keyword","ignore_above": 256}}},
                "in_reply_to_status": {"type" : "object", "enabled": false},
                "in_reply_to_user": {"type" : "object", "enabled": false},
                "favorite_count": {"type" : "object", "enabled": false},
                "lang": {"type" : "object", "enabled": false},
                "is_retweet": {"type" : "object", "enabled": false},
                "coordinate": {"type" : "object", "enabled": false},
                "user_mentions": {"type" : "object", "enabled": false},
                "user.id": {"type" : "object", "enabled": false},
                "user.name": {"type" : "object", "enabled": false},
                "user.screen_name": {"type" : "object", "enabled": false},
                "user.lang": {"type" : "object", "enabled": false},
                "user.location": {"type" : "object", "enabled": false},
                "user.profile_image_url": {"type" : "object", "enabled": false},
                "user.create_at" : {"type": "date", "format": "strict_date_time"},
                "user.description": {"type" : "object", "enabled": false},
                "user.followers_count": {"type": "object", "enabled": false},
                "user.friends_count": {"type": "object", "enabled": false},
                "user.statues_count": {"type": "object", "enabled": false},
                "place.country": {"type": "object", "enabled": false},
                "place.country_code": {"type": "object", "enabled": false},
                "place.bounding_box": {"type" : "object", "enabled": false},
                "place.full_name": {"type": "object", "enabled": false},
                "place.id": {"type": "object", "enabled": false},
                "place.name": {"type": "object", "enabled": false},
                "place.place_type": {"type": "object", "enabled": false},
                "geo_tag.stateName": {"type" : "object", "enabled": false},
                "geo_tag.countyName": {"type" : "object", "enabled": false},
                "geo_tag.cityName": {"type" : "object", "enabled": false},
                "geo_tag.stateID": {"type": "long"},
                "geo_tag.countyID": {"type": "long"},
                "geo_tag.cityID": {"type": "long"}
            }
        }
    },
    "settings": {
        "index": {
	        "max_result_window": 2147483647,
	        "number_of_replicas": 0,
	        "number_of_shards": 4
        }
    }
}
'

printf "\n\n[info] Creating a template named twitter the following schema in Elasticsearch...\nThis template is used for creating view table in Cloudberry...\n\n"
curl -X PUT "localhost:9200/_template/twitter" -H 'Content-Type: application/json' -d'
{
    "index_patterns": ["twitter.ds_tweet_*"],
    "mappings" : {
        "_doc" : {
            "properties" : {
                "create_at" : {"type": "date", "format": "strict_date_time"},
                "text": {"type": "text", "fields": {"keyword": {"type": "keyword","ignore_above": 256}}},
                "id": {"type" : "long"},
                "hashtags": {"type": "text", "fields": {"keyword": {"type": "keyword","ignore_above": 256}}},
                "in_reply_to_status": {"type" : "object", "enabled": false},
                "in_reply_to_user": {"type" : "object", "enabled": false},
                "favorite_count": {"type" : "object", "enabled": false},
                "lang": {"type" : "object", "enabled": false},
                "is_retweet": {"type" : "object", "enabled": false},
                "coordinate": {"type" : "object", "enabled": false},
                "user_mentions": {"type" : "object", "enabled": false},
                "user.id": {"type" : "object", "enabled": false},
                "user.name": {"type" : "object", "enabled": false},
                "user.screen_name": {"type" : "object", "enabled": false},
                "user.lang": {"type" : "object", "enabled": false},
                "user.location": {"type" : "object", "enabled": false},
                "user.profile_image_url": {"type" : "object", "enabled": false},
                "user.create_at" : {"type": "date", "format": "strict_date_time"},
                "user.description": {"type" : "object", "enabled": false},
                "user.followers_count": {"type": "object", "enabled": false},
                "user.friends_count": {"type": "object", "enabled": false},
                "user.statues_count": {"type": "object", "enabled": false},
                "place.country": {"type": "object", "enabled": false},
                "place.country_code": {"type": "object", "enabled": false},
                "place.bounding_box": {"type" : "object", "enabled": false},
                "place.full_name": {"type": "object", "enabled": false},
                "place.id": {"type": "object", "enabled": false},
                "place.name": {"type": "object", "enabled": false},
                "place.place_type": {"type": "object", "enabled": false},
                "geo_tag.stateName": {"type" : "object", "enabled": false},
                "geo_tag.countyName": {"type" : "object", "enabled": false},
                "geo_tag.cityName": {"type" : "object", "enabled": false},
                "geo_tag.stateID": {"type": "long"},
                "geo_tag.countyID": {"type": "long"},
                "geo_tag.cityID": {"type": "long"}
            }
        }
    },
    "settings": {
        "index": {
	        "max_result_window": 2147483647,
	        "number_of_replicas": 0,
	        "number_of_shards": 4,
	        "refresh_interval": "10s"
        }
    }
}
'

printf "\n\n[info] Showing high-level information about indices in the Elasticsearch cluster BEFORE ingesting data...\n\n"
curl -X GET "localhost:9200/_cat/indices?v"

printf "\n[info] Showing information about templates in the Elasticsearch cluster...\n\n"
curl -X GET "localhost:9200/_cat/templates?v&s=name&pretty"

printf "\n[info] Compiling geo tag modules...\n\n"
sbt "project noah" assembly

printf "\n\n[info] Start to ingest tweets...\n\n"
# The first argument after "./geotag.sh" means the number of threads used to ingest data. Feel free to change it to the number of threads your local machine has.
# Run the following command under path: ~/quick-start/cloudberry/examples/twittermap/
# Need to use Python 3.x in the following command.
gunzip -c ./script/sample.json.gz | ./script/elasticGeoTag.sh 4 2>&1 | python3 ./script/ingestElasticData.py

sleep 2 # Waiting for Elasticsearch indexing process
printf "\n\n[info] Showing high-level information about indices in Elasticsearch cluster AFTER ingesting data...\n\n"
curl -X GET "localhost:9200/_cat/indices?v"

printf "\n\n[success] Finish ingesting tweets"
