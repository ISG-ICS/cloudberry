<?php

echo "Connecting to MySql container ... \n";
$host = $argv[1];
$retries = 10;
while ($retries > 0)
{
    try {
        $con = new \PDO('mysql:host='.$host.';port=6603;', 'root', '');
        echo "Connected to mysql-container!\n";
        $retries = 0;
    } catch (PDOException $e) {
        $retries--;
        sleep(5);
    }
}

echo "Creating Database ... \n";
$con->query("create database if not exists `sql`;");
$con->query("use `sql`;");

echo "Creating Table twitter_ds_tweet... \n";
$create_twitter_ds_tweet = "CREATE TABLE IF NOT EXISTS `twitter_ds_tweet` (
  `place.full_name` varchar(255) DEFAULT NULL,
  `place.bounding_box` varchar(255) DEFAULT NULL,
  `place.country_code` varchar(255) DEFAULT NULL,
  `user.friends_count` bigint(20) NOT NULL,
  `user.description` text NOT NULL,
  `favorite_count` bigint(20) NOT NULL,
  `geo_tag.countyID` bigint(20) DEFAULT NULL,
  `user.location` varchar(255) NOT NULL,
  `place.type` varchar(255) DEFAULT NULL,
  `geo_tag.cityName` varchar(255) DEFAULT NULL,
  `user.id` double NOT NULL,
  `geo_tag.stateName` varchar(255) DEFAULT NULL,
  `geo_tag.cityID` double DEFAULT NULL,
  `is_retweet` double NOT NULL,
  `text` text NOT NULL,
  `user.screen_name` varchar(255) NOT NULL,
  `retweet_count` bigint(20) NOT NULL,
  `place.country` varchar(255) DEFAULT NULL,
  `in_reply_to_user` bigint(20) NOT NULL,
  `user.statues_count` varchar(255) NOT NULL,
  `id` bigint(20) NOT NULL,
  `coordinate` point DEFAULT NULL,
  `place.id` varchar(255) DEFAULT NULL,
  `in_reply_to_status` bigint(20) NOT NULL,
  `geo_tag.stateID` bigint(20) DEFAULT NULL,
  `create_at` datetime NOT NULL,
  `user.create_at` datetime NOT NULL,
  `place.name` varchar(255) DEFAULT NULL,
  `lang` varchar(255) NOT NULL,
  `user.lang` varchar(255) NOT NULL,
  `user.name` varchar(255) NOT NULL,
  `geo_tag.countyName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `create_at` (`create_at`),
  FULLTEXT KEY `idx` (`text`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;";

$con->query($create_twitter_ds_tweet);

$handle = @fopen("./script/sample.json", "r");

# set unlimited maximum amount of memory available to PHP
ini_set('memory_limit','-1');

if($handle) {
    echo "Inserting Records ... \n";
    $count = 0;
    while(!feof($handle)) {
        $count += 1;
        if ($count % 1000 == 0) {
            echo "send record: ".$count."\n";
        }

        $line = fgets($handle);
        $row = json_decode($line, true);

        if ($row['coordinate'] == null) {
            $coordinate = "null";
        } else {
            $coordinate = 'ST_GeomFromText("'.$row['coordinate'].'")';
        }
        if ($row['geo_tag']['stateID'] == null) {
            $geo_stateID = $geo_countyID = $geo_cityID = "null";
        } else {
            $geo_stateID = $row['geo_tag']['stateID'];
            $geo_countyID = $row['geo_tag']['countyID'];
            $geo_cityID = $row['geo_tag']['cityID'];
        }

        $create_at = '"'.$row['create_at'].'"';
        $id = $row['id'];
        $text = '"'.str_replace('"','\"',$row['text']).'"';
        $in_reply_to_status = $row['in_reply_to_status'];
        $in_reply_to_user = $row['in_reply_to_user'];
        $favorite_count = $row['favorite_count'];
        $retweet_count = $row['retweet_count'];
        $lang = '"'.$row['lang'].'"';
        $is_retweet = $row['is_retweet'];
        $user_id = $row['user']['id'];
        $user_name = '"'.str_replace('"','\"',$row['user']['name']).'"';
        $user_lang = '"'.$row['user']['lang'].'"';
        $user_screen_name = '"'.str_replace('"','\"',$row['user']['screen_name']).'"';
        $user_location = '"'.($row['user']['location']).'"';
        $user_create_at = '"'.($row['user']['create_at']).'"';
        $user_description = '"'.str_replace('"','\"',$row['user']['description']).'"';
        $user_friends_count = ($row['user']['friends_count']);
        $user_statues_count = ($row['user']['statues_count']);
        $place_country = '"'.$row['place']['country'].'"';
        $place_country_code = '"'.$row['place']['country_code'].'"';
        $place_full_name = '"'.$row['place']['full_name'].'"';
        $place_id = '"'.$row['place']['id'].'"';
        $place_name = '"'.$row['place']['name'].'"';
        $place_type = '"'.$row['place']['place_type'].'"';
        $place_bounding_box = '"'.$row['place']['bounding_box'].'"';
        $geo_stateName = '"'.$row['geo_tag']['stateName'].'"';
        $geo_countyName = '"'.$row['geo_tag']['countyName'].'"';
        $geo_cityName = '"'.$row['geo_tag']['cityName'].'"';

        $query = "REPLACE INTO `twitter_ds_tweet`(
                  coordinate, `place.bounding_box`,
                  create_at, id, text, in_reply_to_status, in_reply_to_user, favorite_count, retweet_count, lang, is_retweet,
                 `user.lang`, `user.id`, `user.name`, `user.screen_name`, `user.location`, `user.create_at`,`user.description`, `user.friends_count`, `user.statues_count`,
                 `place.country`, `place.country_code`, `place.full_name`, `place.name`, `place.id`, `place.type`,
                 `geo_tag.stateID`, `geo_tag.stateName`, `geo_tag.countyID`, `geo_tag.countyName`, `geo_tag.cityID`, `geo_tag.cityName`)
             VALUES(
                 $coordinate, $place_bounding_box, $create_at, $id, $text, $in_reply_to_status, $in_reply_to_user, $favorite_count, $retweet_count, $lang, $is_retweet,
                 $user_lang, $user_id, $user_name, $user_screen_name, $user_location, $user_create_at, $user_description, $user_friends_count, $user_statues_count,
                 $place_country, $place_country_code, $place_full_name, $place_name, $place_id, $place_type,
                 $geo_stateID, $geo_stateName, $geo_countyID, $geo_countyName, $geo_cityID, $geo_cityName
                 )";

        $con->query($query);

    }
    echo "Ingested sample tweets.\n";
}

?>
