# -*- coding: utf-8 -*-
import os
os.environ['NLS_LANG'] = 'SIMPLIFIED CHINESE_CHINA.UTF8'
import cx_Oracle
import json

con = cx_Oracle.connect('berry/orcl@localhost:1521/orcl')
var = con.version.split(".")
print(var)
cur = con.cursor()
try:
    cur.execute("drop table \"twitter.ds_tweet\"")
except cx_Oracle.DatabaseError as e:
    print(e)
    print("\"twitter.ds_tweet\" table does not exists")
print("Starting to create table \"twitter.ds_tweet\"")

createquery =  """CREATE TABLE "twitter.ds_tweet" (
  "place.bounding_box" varchar2(255) DEFAULT NULL,
  "favorite_count" int NOT NULL,
  "geo_tag.countyID" int DEFAULT NULL,
  "user.id" number NOT NULL,
  "geo_tag.cityID" int DEFAULT NULL,
  "is_retweet" int NOT NULL,
  "text" varchar(1000) NOT NULL,
  "retweet_count" int NOT NULL,
  "in_reply_to_user" number NOT NULL,
  "user.status_count" varchar2(255) NOT NULL,
  "user_mentions" varchar2(1000) DEFAULT null, 
  "id" number NOT NULL,
  "coordinate" SDO_GEOMETRY DEFAULT NULL,
  "in_reply_to_status" number NOT NULL,
  "geo_tag.stateID" int DEFAULT NULL,
  "create_at" timestamp NOT NULL,
  "lang" varchar2(255) NOT NULL,
  "user.name" varchar2(255) NOT NULL,
  "hashtags" varchar2 (1500) DEFAULT NULL,
  "user.profile_image_url" varchar2 (255) DEFAULT NULL, 
  PRIMARY KEY ("id")
) """
print(createquery)
cur.execute(createquery)
print("Table created...")
print("Start inserting data...")
with open("sample.json","r",encoding="utf-8",errors="ignore") as f:
    r = f.readlines()
    count = 0
    for i in r:
        if count %1000 == 0:
            print("send record: {}".format(count))

        try:
            tempJson = json.loads(r""+i)
        except json.decoder.JSONDecodeError as De:
            print(De)
            continue
        try:

            coordinate = '''SDO_GEOMETRY(
      2001,
      NULL,
      SDO_POINT_TYPE({}, {}, NULL),
      NULL,
      NULL)'''.format(tempJson['coordinate'].split()[0][6:],tempJson['coordinate'].split()[1][:-1])

        except KeyError:
            coordinate = "null"
        try:
            geo_stateID = tempJson['geo_tag']['stateID']
            geo_countyID = tempJson['geo_tag']['countyID']
            geo_cityID = tempJson['geo_tag']['cityID']
        except KeyError:
            geo_stateID = "null"
            geo_countyID = "null"
            geo_cityID = "null"
        create_at = "null"
        id = "null"
        text = "null"
        in_reply_to_status = "null"
        in_reply_to_user = "null"
        favorite_count ="null"
        retweet_count = "null"
        lang = "null"
        is_retweet = "null"
        user_id = "null"
        user_name = "null"
        user_lang = "null"
        user_screen_name = "null"
        user_location = "null"
        user_create_at = "null"
        user_description = "null"
        user_friends_count = "null"
        user_statues_count = "null"
        user_followers_count = "null"
        place_country = "null"
        place_country_code = "null"
        place_full_name = "null"
        place_id = "null"
        place_name = "null"
        place_type = "null"
        place_bounding_box = "null"
        geo_stateName = "null"
        geo_countyName = "null"
        geo_cityName = "null"
        user_profile_image_url = "'null'"
        user_mentions = "'null'"
        hashtags = "'null'"
        try:
            create_at =  "to_date("+"'"+ tempJson['create_at'].split(".")[0]+"'"+", 'yyyy-mm-dd hh24:mi:ss')"
            id = tempJson['id']
            text = "'"+str(tempJson['text']).replace("'",r"''")+"'"
            in_reply_to_status = tempJson['in_reply_to_status']
            in_reply_to_user = tempJson['in_reply_to_user']
            favorite_count = tempJson['favorite_count']
            retweet_count = tempJson['retweet_count']
            lang = "'"+tempJson['lang']+"'"
            is_retweet = tempJson['is_retweet']
            user_id = tempJson["user"]["id"]
            user_name = "'"+str(tempJson["user"]["name"]).replace("'",r"''")+"'"
            user_lang = "'"+tempJson['user']['lang']+"'"
            user_screen_name = "'"+ str(tempJson['user']['screen_name']).replace("'",r"''")+"'"
            user_location = "'"+ tempJson["user"]["location"] + "'"
            user_create_at = "to_date("+"'"+ tempJson['user']['create_at'].split(".")[0]+"'"+", 'yyyy-mm-dd hh24:mi:ss')"
            user_description = "'"+str(tempJson["user"]["description"]).replace("'",r"''")+"'"
            user_friends_count = tempJson['user']['friends_count']
            user_statues_count = tempJson['user']['statues_count']
            user_followers_count = tempJson['user']['followers_count']
            place_country = "'"+ tempJson['place']['country'] + "'"
            place_country_code = "'"+tempJson['place']['country_code']+"'"
            place_full_name = "'"+tempJson['place']['full_name']+"'"
            place_id = "'"+tempJson['place']['id']+"'"
            place_name = "'"+tempJson['place']['name']+"'"
            place_type = "'"+tempJson['place']['place_type']+"'"
            place_bounding_box = "'"+tempJson['place']['bounding_box']+"'"
            geo_stateName = "'"+tempJson['geo_tag']["stateName"]+"'"
            geo_countyName = "'"+tempJson['geo_tag']["countyName"]+"'"
            geo_cityName = "'"+tempJson['geo_tag']["cityName"]+"'"
            

        except KeyError:
            pass

        insertQuery = """INSERT INTO "twitter.ds_tweet"(
            "create_at","id","coordinate","lang","is_retweet","hashtags","user_mentions","user.id",
            "user.name","user.profile_image_url","geo_tag.stateID","geo_tag.countyID","geo_tag.cityID","place.bounding_box",
            "text","in_reply_to_status","in_reply_to_user","favorite_count","retweet_count","user.status_count") VALUES({},{},
            {},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{})\n
                  """.format(create_at,id,coordinate,lang,is_retweet,hashtags,user_mentions,user_id,user_name,user_profile_image_url,
                             geo_stateID,geo_countyID,geo_cityID,place_bounding_box,text,in_reply_to_status,in_reply_to_user,favorite_count,
                             retweet_count,user_statues_count)
        if count == 1:
            print(insertQuery)
        try:
            cur.execute(insertQuery)
            count += 1
        except Exception as e:
            print(insertQuery)
            print(e)
        
con.commit()

print("Ingestion Completed {} records ingested".format(count))


