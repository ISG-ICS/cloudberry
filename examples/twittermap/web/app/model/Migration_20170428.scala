package model

import play.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

private[model] class Migration_20170428() {

  import Migration_20170428._

  def up(wsClient: WSClient, cloudberryURL: String)(implicit ec: ExecutionContext): Future[Boolean] = {
//    Future.traverse(Seq(TwitterDrugMapDDL, TwitterMapDDL, StatePopulation, CountyPopulation, CityPopulation)) { jsonStr =>
    Future.traverse(Seq(TwitterMapDDL, StatePopulation, CountyPopulation, CityPopulation)) { jsonStr =>
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
      |      {"name":"place.bounding_box","isOptional":false,"datatype":"String"},
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
}
