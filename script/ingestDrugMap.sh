host=${1:-'http://localhost:19002/aql'}
nc=${2:-"nc1"}
# ddl to register the twitter dataset
cat <<EOF | curl -XPOST --data-binary @- $host
use dataverse twitter;
create type typeTweetMoney if not exists as open{
    create_at : datetime,
    id: int64,
    'text': string,
    retweet_count : int64,
    lang : string,
    is_retweet: boolean,
    user : typeUser,
    geo_tag: typeGeoTag
}; 
create dataset ds_tweet_money(typeTweetMoney) if not exists primary key id;

create feed DrugFeed using socket_adapter
(
    ("sockets"="$nc:10002"),
    ("address-type"="nc"),
    ("type-name"="typeTweetMoney"),
    ("format"="adm")
);
connect feed DrugFeed to dataset ds_tweet_money;
start feed DrugFeed;

EOF

echo 'Created drugmap datasets in AsterixDB.'
#Serve socket feed using local file
cat ./script/drugmap.adm | ./script/fileFeed.sh $host 10002
echo 'Ingested drugmap dataset.'


