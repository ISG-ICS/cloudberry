host=${1:-'http://localhost:19002/query/service'}
nc=${2:-"asterix_nc1"}
# ddl to register the twitter dataset
curl -XPOST $host --data-urlencode "statement=use twitter; 

create type typeTweetMoney if not exists as open{ 
    create_at : datetime, 
    id: int64, 
    text: string, 
    retweet_count : int64, 
    lang : string, 
    is_retweet: boolean, 
    user : typeUser, 
    geo_tag: typeGeoTag 
}; 
create dataset ds_tweet_money(typeTweetMoney) if not exists primary key id; 

create feed DrugFeed with { 
    \"adapter-name\" : \"socket_adapter\", 
    \"sockets\" : \"asterix_nc1:10002\", 
    \"address-type\" : \"nc\", 
    \"type-name\" : \"typeTweetMoney\", 
    \"format\" : \"adm\", 
    \"upsert-feed\" : \"false\" 
}; 

connect feed DrugFeed to dataset ds_tweet_money; 
start feed DrugFeed;"

echo 'Created drugmap datasets in AsterixDB.'
#Serve socket feed using local file
cat ./script/drugmap.adm | ./script/fileFeed.sh 127.0.0.1 10002
echo 'Ingested drugmap dataset.'

curl -XPOST $host --data-urlencode "statement=use twitter;  
stop feed DrugFeed;  
drop feed DrugFeed;"
