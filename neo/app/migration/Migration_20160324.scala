package migration

import models.AQLConnection
import play.api.Logger
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class Migration_20160324(val connection: AQLConnection) {

  import Migration_20160324._

  def up(): Future[WSResponse] = {
    Logger.logger.info("Migration create table")
    createDataverse()
    createViewTable()
    createDataTable()
  }

  def down(): Future[WSResponse] = {
    dropDataTable()
    dropViewTable()
    dropDataverse()
  }

  private def createDataverse() = {
    connection.post(
      s"""
         |create dataverse $Dataverse if not exists;
       """.stripMargin
    )
  }

  private def dropDataverse() = {
    post(
      s"""
         |drop dataverse $Dataverse if exists;
       """.stripMargin
    )
  }

  private def createViewTable() = {
    post(
      s"""
         |use dataverse $Dataverse
         |
         |create type type$ViewMetaDataset if not exists as open {
         | "dataset": string,
         | "keyword": string,
         | "timeStart": datetime,
         | "timeEnd": datetime
         |}
         |
         |create dataset $ViewMetaDataset(type$ViewMetaDataset) if not exists primary key "dataset","keyword";
         |
      """.stripMargin)
  }

  private def dropViewTable() = {
    post(
      s"""
         |use dataverse $Dataverse
         |
         |drop dataset $ViewMetaDataset if exists;
      """.stripMargin)

  }

  private def createDataTable() = {
    post(
      s"""
         |use dataverse $Dataverse
         |
         |create type typeUser if not exists as open{
         |    id: int64,
         |    name: string,
         |    screen_name : string,
         |    lang : string,
         |    location: string,
         |    create_at: date,
         |    description: string,
         |    followers_count: int32,
         |    friends_count: int32,
         |    statues_count: int64
         |}
         |
         |create type typePlace if not exists as open{
         |    country : string,
         |    country_code : string,
         |    full_name : string,
         |    id : string,
         |    name : string,
         |    place_type : string,
         |    bounding_box : rectangle
         |}
         |
         |create type typeGeoTag if not exists as open {
         |    stateID: int32,
         |    stateName: string,
         |    countyID: int32,
         |    countyName: string,
         |    cityID: int32?,
         |    cityName: string?
         |}
         |
         |create type typeTweet if not exists as open{
         |    create_at : datetime,
         |    id: int64,
         |    text_msg : string,
         |    in_reply_to_status : int64,
         |    in_reply_to_user : int64,
         |    favorite_count : int64,
         |    coordinate: point?,
         |    retweet_count : int64,
         |    lang : string,
         |    is_retweet: boolean,
         |    hashtags : {{ string }} ?,
         |    user_mentions : {{ int64 }} ? ,
         |    user : typeUser,
         |    place : typePlace?,
         |    geo_tag: typeGeoTag
         |}
         |
         |create dataset ds_tweets(typeTweet) if not exists primary key id;
         |//with filter on create_at;
         |create index text_idx on ds_tweets(text_msg) if not exists type keyword;
         |create index location_idx on ds_tweets(coordinate) if not exists type rtree;
         |create index time_idx on ds_tweets(create_at) if not exists type btree;
         |create index state_idx on ds_tweets(geo_tag.stateID) if not exists type btree;
         |create index county_idx on ds_tweets(geo_tag.countyID) if not exists type btree;
         |create index city_idx on ds_tweets(geo_tag.cityID) if not exists type btree;
         |
         |//create feed fd_tweets using socket_adapter
         |//(
         |//    ("sockets"="nc1:10001"),
         |//    ("address-type"="nc"),
         |//    ("type-name"="type_tweet"),
         |//    ("format"="adm"),
         |//    ("duration"="1200")
         |//);
         |
         |//set wait-for-completion-feed "false";
         |//connect feed fd_tweets to dataset ds_tweets using policy AdvancedFT_Discard;
       """.stripMargin
    )
  }

  def dropDataTable() = ???

  private def post(statement: String): Future[WSResponse] = connection.post(statement)
}

object Migration_20160324 {
  val Dataverse = "twitter"
  val ViewMetaDataset = "viewMeta"

}
