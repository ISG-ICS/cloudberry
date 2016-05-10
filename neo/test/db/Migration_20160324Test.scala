package db

import edu.uci.ics.cloudberry.zion.asterix.AsterixConnection
import org.specs2.mock._
import org.specs2.mutable._

class Migration_20160324Test extends Specification with Mockito {
  val mockConnection = mock[AsterixConnection]

  val migration_20160324 = Migration_20160324.apply(mockConnection)
  "Migration#createDataSet" should {
    "succeed to print out the valid AQL" in {
      println(migration_20160324.createDataverse().trim)
      migration_20160324.createDataverse().trim must equalTo("create dataverse twitter if not exists;")
      println(migration_20160324.createEventDataTable().trim)
      migration_20160324.createEventDataTable().trim must equalTo(
        """
          |use dataverse twitter
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
          |    "text": string,
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
          |create dataset ds_tweet(typeTweet) if not exists primary key id;
          |//with filter on create_at;
          |create index text_idx if not exists on ds_tweet("text") type keyword;
          |create index location_idx if not exists on ds_tweet(coordinate) type rtree;
          |create index time_idx if not exists on ds_tweet(create_at) type btree;
          |create index state_idx if not exists on ds_tweet(geo_tag.stateID) type btree;
          |create index county_idx if not exists on ds_tweet(geo_tag.countyID) type btree;
          |create index city_idx if not exists on ds_tweet(geo_tag.cityID) type btree;
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
          |//connect feed fd_tweets to dataset ds_tweet using policy AdvancedFT_Discard;
        """.stripMargin.trim())
      migration_20160324.dropEventTable().trim() must equalTo(
        """
          |use dataverse twitter
          |
          |drop dataset ds_tweet if exists;
          |drop type typeTweet if exists;
          |drop type typeGeoTag if exists;
          |drop type typePlace if exists;
          |drop type typeUser if exists;
        """.stripMargin.trim()
      )
    }
  }

}
