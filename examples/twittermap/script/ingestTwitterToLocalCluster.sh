#!/bin/bash -
#===============================================================================
#
#          FILE: ingestTwitterToLocalCluster.sh
#
#         USAGE: ./ingestTwitterToLocalCluster.sh
#
#   DESCRIPTION: Ingest the twitter data to AsterixDB
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Jianfeng Jia (), jianfeng.jia@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 10/27/2015 11:06:01 AM PDT
#      REVISION:  ---
#===============================================================================

set -o nounset                              # Treat unset variables as an error

# ddl to register the twitter dataset
host=${1:-"http://localhost:19002/aql"}
nc=${2:-"nc1"}
cat <<EOF | curl -XPOST --data-binary @- $host
create dataverse twitter if not exists;
use dataverse twitter
create type typeUser if not exists as open {
    id: int64,
    name: string,
    screen_name : string,
    profile_image_url : string,
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
// with filter on create_at;
//"using" "compaction" "policy" CompactionPolicy ( Configuration )? )?
create index text_idx if not exists on ds_tweet("text") type fulltext;
//create index time_idx if not exists on ds_tweet(create_at) type btree;
//create index location_idx if not exists on ds_tweet(coordinate) type rtree;
//create index state_idx if not exists on ds_tweet(geo_tag.stateID) type btree;
//create index county_idx if not exists on ds_tweet(geo_tag.countyID) type btree;
//create index city_idx if not exists on ds_tweet(geo_tag.cityID) type btree;

create feed TweetFeed using socket_adapter
(
    ("sockets"="$nc:10001"),
    ("address-type"="nc"),
    ("type-name"="typeTweet"),
    ("format"="adm")
);
connect feed TweetFeed to dataset ds_tweet;
start feed TweetFeed;
EOF


#[ -f ./script/sample.adm.gz ] || { echo "Downloading the data...";  ./script/getSampleTweetsFromGDrive.sh; }

echo "Start ingestion ..." 
gunzip -c ./script/sample.adm.gz | ./script/fileFeed.sh $host 10001
echo "Ingested sample tweets."

