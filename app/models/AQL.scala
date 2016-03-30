package models

import actors.ParsedQuery
import migration.Migration_20160324
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class AQL(val statement: String) {

}

object AQL {

  import Migration_20160324._

  val TweetsType = "type_tweet"
  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  def createView(fromDataSet: DataSet, keyword: String, initialTimeSpan: Interval): String = {
    val viewName = fromDataSet.name + '_' + keyword
    val createStatement =
      s"""
         |use dataverse $Dataverse
         |create dataset $viewName($TweetsType) if not exists primary key id;
     """.stripMargin
    createStatement + appendView(fromDataSet, keyword, initialTimeSpan)
  }


  def toKeywordSelection(query: Any): AQL = ???

  def toTimeIntervalSelection(query: Any): AQL = ???

  def toSpatialSelection(query: Any): AQL = ???

  def appendView(fromDataset: DataSet, keyword: String, interval: Interval): String = {
    val viewName = fromDataset.name + '_' + keyword
    s"""
       |use dataverse $Dataverse
       |insert into dataset $viewName(
       |let $$ts_start := datetime("${TimeFormat.print(interval.getStart)}")
       |let $$ts_end := datetime("${TimeFormat.print(interval.getEnd)}")
       |for $$t in dataset ${fromDataset.name}
       |let $$keyword0 := "$keyword"
       |where $$t.place.place_type = "city"
       |and $$t.create_at >= $$ts_start and $$t.create_at < $$ts_end
       |and contains($$t.text_msg, $$keyword0)
       |return {
       |  "create_at" : $$t.create_at,
       |  "id": $$t.id,
       |  "text_msg" : $$t.text_msg,
       |  "in_reply_to_status" : $$t.in_reply_to_status,
       |  "in_reply_to_user" : $$t.in_reply_to_user,
       |  "favorite_count" : $$t.favorite_count,
       |  "geo_location": $$t.geo_location,
       |  "retweet_count" : $$t.retweet_count,
       |  "lang" : $$t.lang,
       |  "is_retweet": $$t.is_retweet,
       |  "hashtags" :$$t.hashtags,
       |  "user_mentions" : $$t.user_mentions ,
       |  "user" : $$t.user,
       |  "place" : $$t.place,
       |  "state" : substring-after($$t.place.full_name, ", "),
       |  "city" : substring-before($$t.place.full_name, ","),
       |  "county": (for $$city in dataset ds_zip
       |              where substring-before($$t.place.full_name, ",") = $$city.city
       |              and substring-after($$t.place.full_name, ", ") = $$city.state
       |              and not(is-null($$city.county))
       |              return string-concat([$$city.state, "-", $$city.county]) )[0]}
       |)
       |
       """.stripMargin
  }

  def aggregateByMapEntity(query: ParsedQuery): AQL = {
    val viewName = query.key
    val entityPredicate = query.entities.foldLeft("")((pre, e) => pre + s"""or $$t.state = "$e" """)
    new AQL(aggregateByEntityAQL(viewName, entityPredicate.substring(3)))
  }

  def aggregateByTime(query: ParsedQuery): AQL = {
    val viewName = query.key
    val entityPredicate = query.entities.foldLeft("")((pre, e) => pre + s"""or $$t.state = "$e" """)
    new AQL(aggregateByTimeAQL(viewName, entityPredicate.substring(3)))
  }

  def aggregateByEntityAQL(viewName: String, predicate: String): String = {
    s"""
       |use dataverse $Dataverse
       |for $$t in dataset $viewName
       |where $predicate
       |group by $$c := $$t.state with $$t
       |let $$count := count($$t)
       |order by $$count desc
       |return { $$c : $$count };
      """.stripMargin
  }

  def aggregateByTimeAQL(viewName: String, predicate: String): String = {
    s"""
       |use dataverse $Dataverse
       |for $$t in dataset $viewName
       |where $predicate
       |group by $$c := print-datetime($$t.create_at, "YYYY-MM-DD hh") with $$t
       |let $$count := count($$t)
       |order by $$count desc
       |return { $$c : $$count };
      """.stripMargin
  }

  //    |
  //         |for $$t in dataset temp_v5os5udpr
  //         |group by $$c := print-datetime($$t.create_at, "YYYY-MM-DD hh") with $$t
  //         |let $$count := count($$t)
  //         |order by $$c
  //         |return {$$c : $$count };
  //         |
  //         |for $$t in dataset temp_v5os5udpr
  //         |where not(is-null($$t.hashtags))
  //         |for $$h in $$t.hashtags
  //         |group by $$tag := $$h with $$h
  //         |let $$c := count($$h)
  //         |order by $$c desc
  //         |limit 50
  //         |return { $$tag : $$c};
  //         |
  //         |for $$t in dataset temp_v5os5udpr
  //         |limit 100
  //         |return {$$t.user.screen_name : $$t.text_msg};
  //         |

  def getView(name: String, keyword: String): String = {
    s"""
       |use dataverse $Dataverse
       |for $$d in dataset $ViewMetaDataset
       |where $$d."dataset" = "$name" and $$d."keyword" = "$keyword"
       |return $$d
     """.stripMargin
  }

  def updateViewMeta(dsName: String, keyword: String, interval: Interval): AQL = {
    new AQL(
      s"""
         |use dataverse $Dataverse
         |upsert into dataset $ViewMetaDataset (
         |{
         |   "dataset" : "$dsName",
         |   "keyword" : "$keyword",
         |   "timeStart" : datetime("${TimeFormat.print(interval.getStart)}"),
         |   "timeEnd" : datetime("${TimeFormat.print(interval.getEnd)}")
         |})
       """.stripMargin
    )
  }
}

class AQLConnection(wSClient: WSClient, url: String) {

  def post(aql: String): Future[WSResponse] = {
    Logger.logger.debug("AQL:" + aql)
    val f = wSClient.url(url).post(aql)
    f.onFailure(failureHandler(aql))
    f
  }

  protected def failureHandler(aql: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => Logger.logger.error("WS Error:" + aql, e)
  }
}
