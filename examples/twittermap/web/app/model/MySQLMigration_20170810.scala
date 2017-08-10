package model

import play.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

private[model] class MySqlMigration_20170810() {

  import MySqlMigration_20170810._

  def up(wsClient: WSClient, cloudberryURL: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future.traverse(Seq(TwitterDsTweet)) { jsonStr =>
      wsClient.url(cloudberryURL).withHeaders("Content-Type" -> "application/json").post(jsonStr).map { response =>
        if (response.status % 100 == 2) {
          true
        } else {
          Logger.info(response.statusText + ":" + response.body)
          false
        }
      }
    }.map(seqBoolean => seqBoolean.forall(_ == true))
  }
}

object MySqlMigration_20170810 {

  val migration = new MySqlMigration_20170810()

  val TwitterDsTweet: String =
    """
      |{
      |    "dataset":"twitter_ds_tweet",
      |    "schema": {
      |       "typeName" : "twitter_ds_tweet",
      |       "dimension" : [
      |            { "name" : "create_at", "isOptional" : false, "datatype" : "Time" },
      |            { "name" : "id", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "lang", "isOptional" : false, "datatype" : "String" },
      |            { "name" : "is_retweet", "isOptional" : false, "datatype" : "Boolean"},
      |            { "name" : "coordinate", "isOptional" : true, "datatype" : "Point"}
      |       ],
      |      "measurement" : [
      |            { "name" : "text", "isOptional" : false, "datatype" : "Text" },
      |            { "name" : "in_reply_to_status", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "in_reply_to_user", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "favorite_count", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "retweet_count", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "user.lang", "isOptional" : false, "datatype" : "String" },
      |            { "name" : "user.id", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "user.name", "isOptional" : false, "datatype" : "String" },
      |            { "name" : "user.screen_name", "isOptional" : false, "datatype" : "String" },
      |            { "name" : "user.location",  "isOptional" : false, "datatype" : "String" },
      |            { "name" : "user.create_at", "isOptional" : false, "datatype" : "Time" },
      |            { "name" : "user.description", "isOptional" : false, "datatype" : "Text" },
      |            { "name" : "user.friends_count", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "user.statues_count", "isOptional" : false, "datatype" : "Number" },
      |            { "name" : "place.bounding_box", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "place.country", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "place.country_code", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "place.full_name", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "place.name", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "place.id", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "place.type", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "geo_tag.stateID", "isOptional" : true, "datatype" : "Number" },
      |            { "name" : "geo_tag.countyID", "isOptional" : true, "datatype" : "Number" },
      |            { "name" : "geo_tag.cityID", "isOptional" : true, "datatype" : "Number" },
      |            { "name" : "geo_tag.stateName", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "geo_tag.countyName", "isOptional" : true, "datatype" : "String" },
      |            { "name" : "geo_tag.cityName", "isOptional" : true, "datatype" : "String" }
      |      ],
      |      "primaryKey" : [ "id" ],
      |      "timeField" : "create_at"
      |    }
      |}
    """.stripMargin
}