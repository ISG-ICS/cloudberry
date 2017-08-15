#!/usr/bin/php
#===============================================================================
#
#          FILE: ingestMySqlTwitterToLocalCluster.sh
#
#         USAGE: ./ingestMySqlTwitterToLocalCluster.sh
#
#   DESCRIPTION: Ingest the sample Twitter data to MySqlDB
#
#       OPTIONS:
#  REQUIREMENTS: ---
#          BUGS: ---
#         NOTES: ---
#        AUTHOR: Haohan Zhang (), hh1680651@gmail.com
#  ORGANIZATION: ics.uci.edu
#       CREATED: 08/14/2017 23:24:01 PM PDT
#      REVISION:  ---
#===============================================================================

<?php

# mysql params
$USERNAME = "root";
$PASSWORD = "";
$PORT = 3308;
$dbname = "`sql`";

# start docker machine
echo "Starting docker machine...\n";
shell_exec("docker-machine create --driver virtualbox default");
shell_exec("docker-machine start");
shell_exec("eval $(docker-machine env)");
$HOST = trim(shell_exec("docker-machine ip"));

# create mysql container
echo "Creating docker mysql container...\n";
shell_exec("docker run --name mysql-container -p $PORT:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -d mysql:latest");
shell_exec("docker start mysql-container");

# set up config files (in cloudberry):
shell_exec("sed -i -e 's/localhost:3306/$HOST:$PORT/g' ../../cloudberry/neo/conf/application.conf");
shell_exec("sed -i -e 's/asterixdb.lang = SQLPP/##asterixdb.lang = SQLPP/g' ../../cloudberry/neo/conf/application.conf");
shell_exec("sed -i -e 's/#asterixdb.lang = SQL/asterixdb.lang = SQL/g' ../../cloudberry/neo/conf/application.conf");

echo "Start ingestion ...\n";
shell_exec("unzip -o ./script/mysqlSample.json -d ./script/") or die("cannot unzip!");

echo "Connecting MySQL: $HOST:$PORT ... \n";
$con = mysqli_connect($HOST.":".$PORT, $USERNAME, $PASSWORD) or die('Error in Connecting: ' . mysqli_error($con));

echo "Creating Database ... \n";
$con->query("create database if not exists $dbname;") or die($con->error);
$con->query("use $dbname;") or die($con->error);

# schema for twitter_ds_tweet
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
  `text` text CHARACTER SET latin1 NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8";

$con->query($create_twitter_ds_tweet) or die($con->$error);

echo "Reading sample tweets ... \n";
$st = mysqli_prepare($con,
    'REPLACE INTO `twitter_ds_tweet`(
     coordinate, `place.bounding_box`,
     create_at, id, text, in_reply_to_status, in_reply_to_user, favorite_count, retweet_count, lang, is_retweet,
    `user.lang`, `user.id`, `user.name`, `user.screen_name`, `user.location`, `user.create_at`,`user.description`, `user.friends_count`, `user.statues_count`,
    `place.country`, `place.country_code`, `place.full_name`, `place.name`, `place.id`, `place.type`,
    `geo_tag.stateID`, `geo_tag.stateName`, `geo_tag.countyID`, `geo_tag.countyName`, `geo_tag.cityID`, `geo_tag.cityName`) VALUES
    (ST_GeomFromText(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')  or die(mysqli_error($con));

mysqli_stmt_bind_param($st, 'ssssssssssssssssssssssssssssssss',
    $coordinate, $place_bounding_box,
    $create_at, $id, $text, $in_reply_to_status, $in_reply_to_user, $favorite_count, $retweet_count, $lang, $is_retweet,
    $user_lang, $user_id, $user_name, $user_screen_name, $user_location, $user_create_at, $user_description, $user_friends_count, $user_statues_count,
    $place_country, $place_country_code, $place_full_name, $place_name, $place_id, $place_type,
    $geo_stateID, $geo_stateName, $geo_countyID, $geo_countyName, $geo_cityID, $geo_cityName);

$handle = @fopen("./script/mysqlSample.json", "r");

ini_set('memory_limit','-1');

# insert records
if($handle) {
    echo "Inserting Records ... \n";
    while(!feof($handle)) {
        $line = fgets($handle);
        $row = json_decode($line, true);
        $create_at = $row['create_at'];
        $id = $row['id'];
        $text = $row['text'];
        $coordinate = $row['coordinate'];
        $in_reply_to_status = $row['in_reply_to_status'];
        $in_reply_to_user = $row['in_reply_to_user'];
        $favorite_count = $row['favorite_count'];
        $retweet_count = $row['retweet_count'];
        $lang = $row['lang'];
        $is_retweet = $row['is_retweet'];

        $user_id = $row['user']['id'];
        $user_name = $row['user']['name'];
        $user_lang = $row['user']['lang'];
        $user_screen_name = $row['user']['screen_name'];
        $user_location = ($row['user']['location']);
        $user_create_at = ($row['user']['create_at']);
        $user_description = ($row['user']['description']);
        $user_friends_count = ($row['user']['friends_count']);
        $user_statues_count = ($row['user']['statues_count']);

        $place_country = $row['place']['country'];
        $place_country_code = $row['place']['country_code'];
        $place_full_name = $row['place']['full_name'];
        $place_id = $row['place']['id'];
        $place_name = $row['place']['name'];
        $place_type = $row['place']['place_type'];
        $place_bounding_box = $row['place']['bounding_box'];

        $geo_stateID = ($row['geo_tag']['stateID']);
        $geo_stateName = ($row['geo_tag']['stateName']);
        $geo_countyID = ($row['geo_tag']['countyID']);
        $geo_countyName = ($row['geo_tag']['countyName']);
        $geo_cityID = ($row['geo_tag']['cityID']);
        $geo_cityName = ($row['geo_tag']['cityName']);

    	mysqli_stmt_execute($st);
    	echo mysqli_stmt_error($st);
    }
}

echo "Ingested sample tweets.\n";

mysqli_close($con);
?>
