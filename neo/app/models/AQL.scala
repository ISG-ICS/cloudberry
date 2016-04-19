package models

import actors.CacheQuery
import migration.Migration_20160324
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.concurrent.Execution.Implicits._

import edu.uci.ics.cloudberry.gnosis._

import scala.concurrent.Future

//TODO generalize the API to make this DB agnostic
class AQL(val statement: String) {

}

object AQL {

  import Migration_20160324._

  val TweetsType = "typeTweet"
  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  def createView(fromDataSet: DataSet, keyword: String, initialTimeSpan: Interval): String = {
    val viewName = fromDataSet.name + '_' + keyword
    val createStatement =
      s"""
         |use dataverse $Dataverse
         |create dataset $viewName($TweetsType) if not exists primary key id;
     """.stripMargin
    createStatement + appendView(fromDataSet, keyword, Seq(initialTimeSpan))
  }


  def toKeywordSelection(query: Any): AQL = ???

  def toTimeIntervalSelection(query: Any): AQL = ???

  def toSpatialSelection(query: Any): AQL = ???

  def appendView(fromDataset: DataSet, keyword: String, intervals: Seq[Interval]): String = {
    val viewName = fromDataset.name + '_' + keyword
    val predicate = intervals.map(interval =>
                                    s"""
                                       |$$t.create_at >= datetime("${TimeFormat.print(interval.getStart)}")
                                       |and $$t.create_at < datetime("${TimeFormat.print(interval.getEnd)}")
       """.stripMargin).mkString(" or ")
    s"""
       |use dataverse $Dataverse
       |create dataset $viewName($TweetsType) if not exists primary key id;
       |insert into dataset $viewName(
       |for $$t in dataset ${fromDataset.name}
       |let $$keyword0 := "$keyword"
       |where ($predicate)
       |and similarity-jaccard(word-tokens($$t."text"), word-tokens($$keyword0)) > 0.0
       |return $$t)
       """.stripMargin
  }


  private def formTimePredicate(interval: Interval): String = {
    s"""
       | $$t.create_at >= datetime("${TimeFormat.print(interval.getStart)}")
       | and $$t.create_at < datetime("${TimeFormat.print(interval.getEnd)}")
    """.stripMargin
  }

  val GeoTag = "geo_tag"

  private def getGeoIDName(level: TypeLevel): String = {
    val field = level match {
      case StateLevel => "stateID"
      case CountyLevel => "countyID"
      case CityLevel => "cityID"
    }
    s"$GeoTag.$field"
  }

  private def extracEntityPredicate(level: TypeLevel, entity: IEntity, variable: String): String = {
    val field = s"$$$variable.${getGeoIDName(level)}"
    level match {
      case StateLevel => s"""$field = ${entity.asInstanceOf[USStateEntity].stateID} """
      case CountyLevel => s"""$field = ${entity.asInstanceOf[USCountyEntity].countyID} """
      case CityLevel => s"""$field = ${entity.asInstanceOf[USCityEntity].cityID} """
    }
  }

  def aggregateBy(query: CacheQuery, groupField: String): AQL = {
    val viewName = query.key
    val entityPredicate = query.entities.foldLeft("")((pre, e) => pre + s"""or ${extracEntityPredicate(query.level, e, "t")} """)
    val timePredicate = formTimePredicate(query.timeRange)
    val predicate = s"$timePredicate and (${entityPredicate.substring(3)})"
    groupField.toLowerCase match {
      case "map" =>
        new AQL(aggregateByEntityAQL(query.level, viewName, predicate))
      case "time" =>
        new AQL(aggregateByTimeAQL(viewName, predicate))
      case "hashtag" =>
        new AQL(aggregateByHashTag(viewName, predicate))
    }
  }

  private def groupbyField(level: TypeLevel, variable: String): String = {
    level match {
      case StateLevel => s"$$$variable.$GeoTag.stateName"
      case CountyLevel => s""" string-concat([$$$variable.$GeoTag.stateName, "-", $$$variable.$GeoTag.countyName]) """
      case CityLevel => s""" string-concat([$$$variable.$GeoTag.stateName, "-", $$$variable.$GeoTag.countyName, "-", $$$variable.$GeoTag.cityName]) """
    }
  }

  //TODO make three aggregation as one query
  def aggregateByEntityAQL(level: TypeLevel, viewName: String, predicate: String): String = {
    s"""
       |use dataverse $Dataverse
       |for $$t in dataset $viewName
       |where $predicate
       |let $$cat := ${groupbyField(level, "t")}
       |/* +hash */
       |group by $$c := $$cat with $$t
       |return { $$c : count($$t) };
      """.stripMargin
  }

  def aggregateByTimeAQL(viewName: String, predicate: String): String = {
    s"""
       |use dataverse $Dataverse
       |for $$t in dataset $viewName
       |where $predicate
       |group by $$c := print-datetime($$t.create_at, "YYYY-MM") with $$t
       |let $$count := count($$t)
       |return { $$c : $$count };
      """.stripMargin
  }

  def aggregateByHashTag(viewName: String, predicate: String): String = {
    s"""
       |use dataverse $Dataverse
       |for $$t in dataset $viewName
       |where $predicate
       |and not(is-null($$t.hashtags))
       |for $$h in $$t.hashtags
       |group by $$tag := $$h with $$h
       |let $$c := count($$h)
       |order by $$c desc
       |limit 50
       |return { $$tag : $$c};
      """.stripMargin
  }

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
