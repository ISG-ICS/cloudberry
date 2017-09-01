<?php

echo "Connecting to PostgreSQL container ... \n";
$host = $argv[1];
$retries = 10;
while ($retries > 0)
{
    try {
        $con = new \PDO('pgsql:port=5555;host='.$host.';user=postgres;password=pwd;');
        echo "Connected to postgres-container!\n";
        $retries = 0;
    } catch (PDOException $e) {
        $retries--;
        sleep(5);
    }
}

echo "Creating table twitter_ds_tweet ... \n";
$create = <<<EOT
create table if not exists "twitter_ds_tweet" (
             "place.full_name" varchar(255) default null,
             "place.bounding_box" line default null,
             "place.country_code" varchar(255) default null,
             "user.friends_count" bigint not null,
             "user.description" text not null,
             "favorite_count" bigint not null,
             "geo_tag.countyID" bigint default null,
             "user.location" varchar(255) not null,
             "user_mentions" bigint[] default null,
             "place.type" varchar(255) default null,
             "geo_tag.cityName" varchar(255) default null,
             "user.id" bigint not null,
             "geo_tag.stateName" varchar(255) default null,
             "geo_tag.cityID" bigint default null,
             "is_retweet" smallint not null,
             "text" text not null,
             "user.screen_name" varchar(255) not null,
             "retweet_count" bigint not null,
             "place.country" varchar(255) default null,
             "in_reply_to_user" bigint not null,
             "user.statues_count" bigint not null,
             "id" bigint not null,
             "coordinate" point default null,
             "place.id" varchar(255) default null,
             "in_reply_to_status" bigint not null,
             "geo_tag.stateID" bigint default null,
             "create_at" timestamp not null,
             "user.create_at" timestamp not null,
             "place.name" varchar(255) default null,
             "lang" varchar(255) not null,
             "user.lang" varchar(255) not null,
             "user.name" varchar(255) not null,
             "geo_tag.countyName" varchar(255) default null,
             "hashtags" varchar[] default null, primary key ("id")
           );
EOT;

$con->query($create) or die("Fail to create table!");
echo "Created table twitter_ds_tweet! \n";

$handle = @fopen("./script/sample.json", "r");

# set unlimited maximum amount of memory available to PHP
ini_set('memory_limit','-1');

if($handle)
{
    for($i = 1; ! feof($handle); $i++) {
        if ($i%1000 == 0) {
            echo "send records: ".$i."\n";
        }
        $line = fgets($handle);
        $row = json_decode($line, true);
        $create_at = "'".$row['create_at']."'";
        $id = $row['id'];
        $text = "'".str_replace("'","''",$row['text'])."'";
        $in_reply_to_status = $row['in_reply_to_status'];
        $in_reply_to_user = $row['in_reply_to_user'];
        $favorite_count = $row['favorite_count'];
        $retweet_count = $row['retweet_count'];
        $lang = "'".$row['lang']."'";
        $is_retweet = $row['is_retweet'];
        $user_id = $row['user']['id'];
        $user_name = "'".str_replace("'","''",$row['user']['name'])."'";
        $user_lang = "'".$row['user']['lang']."'";
        $user_screen_name = "'".str_replace("'","''",$row['user']['screen_name'])."'";
        $user_location = "'".($row['user']['location'])."'";
        $user_create_at = "'".($row['user']['create_at'])."'";
        $user_description = "'".str_replace("'","''",$row['user']['description'])."'";
        $user_friends_count = ($row['user']['friends_count']);
        $user_statues_count = ($row['user']['statues_count']);
        $place_country = "'".$row['place']['country']."'";
        $place_country_code = "'".$row['place']['country_code']."'";
        $place_full_name = "'".$row['place']['full_name']."'";
        $place_id = "'".$row['place']['id']."'";
        $place_name = "'".$row['place']['name']."'";
        $place_type = "'".$row['place']['place_type']."'";
        $place_bounding_box = str_replace(array("LINESTRING(",")"," ",",","?"),array("'","'","?"," ",","),$row['place']['bounding_box']);
        $geo_stateName = "'".$row['geo_tag']['stateName']."'";
        $geo_countyName = "'".$row['geo_tag']['countyName']."'";
        $geo_cityName = "'".$row['geo_tag']['cityName']."'";

        if ($row['geo_tag']['stateID'] == null) {
            $geo_stateID = $geo_countyID = $geo_cityID = "null";
        } else {
            $geo_stateID = $row['geo_tag']['stateID'];
            $geo_countyID = $row['geo_tag']['countyID'];
            $geo_cityID = $row['geo_tag']['cityID'];
        }
        $arr = $row['coordinate'];
        if ($arr == null) {$coordinate = 'null';} else {
            $coordinate = str_replace(array("point(",")"," ",",","?"),array("'","'","?"," ",","),$arr);
        }
        $arr = $row['hashtags'];
        if ($arr == null) {$hashtags = 'null';} else {
            $hashtags = "'{".implode(",",$arr)."}'";
        }
        $arr = $row['user_mentions'];
        if ($arr == null) {$user_mentions = 'null';} else {
            $user_mentions = "'{".implode(",",$arr)."}'";
        }

        $query = 'INSERT INTO "twitter_ds_tweet"(
            coordinate, "place.bounding_box", create_at, id, text, in_reply_to_status, in_reply_to_user, favorite_count, retweet_count, lang, is_retweet,
            "user.lang", "user.id", "user.name", "user.screen_name", "user.location", "user.create_at","user.description", "user.friends_count", "user.statues_count",
            "place.country", "place.country_code", "place.full_name", "place.name", "place.id", "place.type",
            "geo_tag.stateID", "geo_tag.stateName", "geo_tag.countyID", "geo_tag.countyName", "geo_tag.cityID", "geo_tag.cityName", "hashtags", "user_mentions"
            ) VALUES (
            '.$coordinate.','.$place_bounding_box.','.$create_at.', '.$id.', '.$text.', '.$in_reply_to_status.', '.$in_reply_to_user.', '.$favorite_count.', '.$retweet_count.', '.$lang.', '.$is_retweet.',
            '.$user_lang.', '.$user_id.', '.$user_name.', '.$user_screen_name.', '.$user_location.', '.$user_create_at.', '.$user_description.', '.$user_friends_count.', '.$user_statues_count.',
            '.$place_country.', '.$place_country_code.', '.$place_full_name.', '.$place_name.', '.$place_id.', '.$place_type.',
            '.$geo_stateID.', '.$geo_stateName.', '.$geo_countyID.', '.$geo_countyName.', '.$geo_cityID.', '.$geo_cityName.', '.$hashtags.', '.$user_mentions.')';

        $st = $con->query($query);
    }
    echo "Ingested sample tweets.\n";
}

?>
