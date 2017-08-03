package model

import play.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

private[model] class Migration_20170428() {

  import Migration_20170428._

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

object Migration_20170428 {
  val migration = new Migration_20170428()

  val TwitterDrugMapDDL: String =
    """
      |{
      |  "dataset":"twitter.ds_tweet_money",
      |  "schema":{
      |  	"typeName":"twitter.typeTweetMoney",
      |    "dimension":[
      |      {"name":"create_at","isOptional":false,"datatype":"Time"},
      |      {"name":"id","isOptional":false,"datatype":"Number"},
      |      {"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},
      |      {"name":"user.id","isOptional":false,"datatype":"Number"},
      |      {"name":"geo_tag.stateID","isOptional":false,"datatype":"Number"},
      |      {"name":"geo_tag.countyID","isOptional":false,"datatype":"Number"},
      |      {"name":"geo_tag.cityID","isOptional":false,"datatype":"Number"},
      |      {"name":"geo","isOptional":false,"datatype":"Hierarchy","innerType":"Number",
      |        "levels":[
      |          {"level":"state","field":"geo_tag.stateID"},
      |          {"level":"county","field":"geo_tag.countyID"},
      |          {"level":"city","field":"geo_tag.cityID"}]}
      |    ],
      |    "measurement":[
      |      {"name":"text","isOptional":false,"datatype":"Text"},
      |      {"name":"money","isOptional":false,"datatype":"Number"}
      |    ],
      |    "primaryKey":["id"],
      |    "timeField":"create_at"
      |  }
      |}
    """.stripMargin

  val TwitterMapDDL: String =
    """
      |{
      |  "dataset":"twitter.ds_tweet",
      |  "schema":{
      |  	"typeName":"twitter.typeTweet",
      |    "dimension":[
      |      {"name":"create_at","isOptional":false,"datatype":"Time"},
      |      {"name":"id","isOptional":false,"datatype":"Number"},
      |      {"name":"coordinate","isOptional":false,"datatype":"Point"},
      |      {"name":"lang","isOptional":false,"datatype":"String"},
      |      {"name":"is_retweet","isOptional":false,"datatype":"Boolean"},
      |      {"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},
      |      {"name":"user_mentions","isOptional":true,"datatype":"Bag","innerType":"Number"},
      |      {"name":"user.id","isOptional":false,"datatype":"Number"},
      |      {"name":"geo_tag.stateID","isOptional":false,"datatype":"Number"},
      |      {"name":"geo_tag.countyID","isOptional":false,"datatype":"Number"},
      |      {"name":"geo_tag.cityID","isOptional":false,"datatype":"Number"},
      |      {"name":"geo","isOptional":false,"datatype":"Hierarchy","innerType":"Number",
      |        "levels":[
      |          {"level":"state","field":"geo_tag.stateID"},
      |          {"level":"county","field":"geo_tag.countyID"},
      |          {"level":"city","field":"geo_tag.cityID"}]}
      |    ],
      |    "measurement":[
      |      {"name":"text","isOptional":false,"datatype":"Text"},
      |      {"name":"in_reply_to_status","isOptional":false,"datatype":"Number"},
      |      {"name":"in_reply_to_user","isOptional":false,"datatype":"Number"},
      |      {"name":"favorite_count","isOptional":false,"datatype":"Number"},
      |      {"name":"retweet_count","isOptional":false,"datatype":"Number"},
      |      {"name":"user.status_count","isOptional":false,"datatype":"Number"}
      |    ],
      |    "primaryKey":["id"],
      |    "timeField":"create_at"
      |  }
      |}
    """.stripMargin

  val StatePopulation: String =
    """
      |{
      |    "dataset": "twitter.dsStatePopulation",
      |    "schema": {
      |        "typeName": "twitter.typeStatePopulation",
      |        "dimension": [
      |            { "name": "name", "isOptional": false, "datatype": "String" },
      |            { "name": "stateID", "isOptional": false, "datatype": "Number" },
      |            { "name": "create_at", "isOptional": false, "datatype": "Time" }
      |        ],
      |        "measurement": [
      |            { "name": "population", "isOptional": false, "datatype": "Number" }
      |        ],
      |        "primaryKey": ["stateID"]
      |    }
      |}
    """.stripMargin

  val CountyPopulation: String =
    """
      |{
      |    "dataset": "twitter.dsCountyPopulation",
      |    "schema": {
      |        "typeName": "twitter.typeCountyPopulation",
      |        "dimension": [
      |            { "name": "name", "isOptional": false, "datatype": "String" },
      |            { "name": "countyID", "isOptional": false, "datatype": "Number" },
      |            { "name": "create_at", "isOptional": false, "datatype": "Time" }
      |        ],
      |        "measurement": [
      |            { "name": "population", "isOptional": false, "datatype": "Number" }
      |        ],
      |        "primaryKey": ["countyID"]
      |    }
      |}
    """.stripMargin

  val CityPopulation: String =
    """
      |{
      |    "dataset": "twitter.dsCityPopulation",
      |    "schema": {
      |        "typeName": "twitter.typeCityPopulation",
      |        "dimension": [
      |            { "name": "name", "isOptional": false, "datatype": "String" },
      |            { "name": "cityID", "isOptional": false, "datatype": "Number" },
      |            { "name": "create_at", "isOptional": false, "datatype": "Time" }
      |        ],
      |        "measurement": [
      |            { "name": "population", "isOptional": false, "datatype": "Number" }
      |        ],
      |        "primaryKey": ["cityID"]
      |    }
      |}
    """.stripMargin

  val TwitterDsTweet: String =
    """
      |{
      |  "dataset":"twitter_ds_tweet",
      |  "schema" : {
      |      "typeName" : "twitter_ds_tweet",
      |      "dimension" : [ {
      |        "name" : "create_at",
      |        "isOptional" : false,
      |        "datatype" : "Time"
      |      }, {
      |        "name" : "id",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "lang",
      |        "isOptional" : false,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "is_retweet",
      |        "isOptional" : false,
      |        "datatype" : "Boolean"
      |      }, {
      |        "name" : "hashtags",
      |        "innerType": "String",
      |        "isOptional": true,
      |        "datatype" : "Bag"
      |      }, {
      |        "name" : "user_mentions",
      |        "innerType": "String",
      |        "isOptional": true,
      |        "datatype" : "Bag"
      |      }],
      |      "measurement" : [ {
      |        "name" : "text",
      |        "isOptional" : false,
      |        "datatype" : "Text"
      |      }, {
      |        "name" : "in_reply_to_status",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "in_reply_to_user",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "favorite_count",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "retweet_count",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "user.lang",
      |        "isOptional" : false,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "user.id",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "user.name",
      |        "isOptional" : false,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "user.screen_name",
      |        "isOptional" : false,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "user.location",
      |        "isOptional" : false,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "user.create_at",
      |        "isOptional" : false,
      |        "datatype" : "Time"
      |      }, {
      |        "name" : "user.description",
      |        "isOptional" : false,
      |        "datatype" : "Text"
      |      }, {
      |        "name" : "user.friends_count",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "user.statues_count",
      |        "isOptional" : false,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "place.bounding_box",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "place.country",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "place.country_code",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "place.full_name",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "place.name",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "place.id",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "place.type",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "geo_tag.stateID",
      |        "isOptional" : true,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "geo_tag.countyID",
      |        "isOptional" : true,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "geo_tag.cityID",
      |        "isOptional" : true,
      |        "datatype" : "Number"
      |      }, {
      |        "name" : "geo_tag.stateName",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "geo_tag.countyName",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      }, {
      |        "name" : "geo_tag.cityName",
      |        "isOptional" : true,
      |        "datatype" : "String"
      |      } ],
      |      "primaryKey" : [ "id" ],
      |      "timeField" : "create_at"
      |    }
      |}
    """.stripMargin
}
