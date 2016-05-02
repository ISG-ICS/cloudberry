package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api._
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class TwitterViewMetaStore(conn: AsterixConnection) extends ViewMetaStore {
  private val views = mutable.Map.empty[String, DataStoreView]

  /**
    * The non-keyword view will be generated as "twitter_"
    *
    * @param source
    * @param query
    * @return
    */
  def getViewName(source: DataStore, query: DBQuery) = {
    val keyword = query.predicates.find(_.isInstanceOf[KeywordPredicate]).map(_.asInstanceOf[KeywordPredicate].keywords.head).getOrElse("")
    source.name + "_" + keyword
  }

  import TwitterViewMetaStore._

  private def buildSubSetView(key: String, source: DataStore, keyword: String): Future[DataStoreView] = {
    val fResponse = conn.post(generateSubSetViewAQL(source.name, keyword))
    fResponse.map { ws =>
      val view = new TwitterKeywordView(conn, source.asInstanceOf[TwitterDataStore], keyword, queryTemplate = keywordViewTemplate(keyword),
                                        startTime = new DateTime(0l), lastVisitTime = new DateTime(), lastUpdateTime = new DateTime(),
                                        visitTimes = 0)
      views.getOrElseUpdate(key, view)
    }
  }

  private def buildSummaryView(key: String, source: DataStore, query: DBQuery): Future[DataStoreView] = {
    val fResponse = conn.post(generateSummaryViewAQL(source.name, query.summaryLevel))
    fResponse.map { ws =>
      val view = new TwitterCountyDaySummaryView(conn, source.asInstanceOf[TwitterDataStore], summaryViewTemplate(query.summaryLevel),
                                                 startTime = new DateTime(0l), lastVisitTime = new DateTime(), lastUpdateTime = new DateTime(),
                                                 visitTimes = 0)
      views.getOrElseUpdate(key, view)
    }
  }

  override def getOrCreateViewStore(source: DataStore, query: DBQuery): Future[DataStoreView] = {
    val key = getViewName(source, query)
    views.get(key) match {
      case Some(view) => Future {
        view
      }
      case None =>
        val optKeyword = query.predicates.find(_.isInstanceOf[KeywordPredicate]).map(_.asInstanceOf[KeywordPredicate])
        if (optKeyword.isDefined) {
          buildSubSetView(key, source, optKeyword.get.keywords.head)
        } else {
          buildSummaryView(key, source, query)
        }
    }
  }

}

object TwitterViewMetaStore {

  val ViewMetaDataSetName = "twitter.viewMeta"
  val ViewMetaCreateAQL =
    s"""
       |create type typeViewMeta if not exists as open {
       |  sourceName: string,
       |  viewKey: string
       |}
       |create dataset ${ViewMetaDataSetName} if not exists primary key viewKey
     """.stripMargin

  def loadFromMetaStore(conn: AsterixConnection): Future[TwitterViewMetaStore] = {
    val aql =
      s"""
         |for $$t in dataset $ViewMetaDataSetName return $$t
       """.stripMargin
    conn.post(aql).map { ws =>
      val viewMetaStore = new TwitterViewMetaStore(conn)
      if (ws.status == 200) {
        ws.json.as[Seq[ViewMetaRecord]].foreach { record =>
          viewMetaStore.views.update(record.viewKey, {
            if (record.viewKey.endsWith("_")) {
              new TwitterCountyDaySummaryView(conn, TwitterDataStore(conn), summaryViewTemplate(record.summaryLevel.get),
                                              record.startTime, record.lastVisitTime, record.lastUpdateTime, record.visitTimes, record.updateCycle)
            } else {
              val keyword = record.viewKey.substring(record.viewKey.lastIndexOf('_') + 1)
              new TwitterKeywordView(conn, TwitterDataStore(conn), keyword, keywordViewTemplate(keyword),
                                     record.startTime, record.lastVisitTime, record.lastUpdateTime, record.visitTimes, record.updateCycle)
            }
          })
        }
      }
      viewMetaStore
    }
  }

  def flushMetaToStore(metaStore: TwitterViewMetaStore) = Future {
    val viewsToRecord = metaStore.views.values.map { view =>
      view match {
        case v: TwitterCountyDaySummaryView => ViewMetaRecord(v.source.name, v.name, Some(v.summaryLevel),
                                                              v.startTime, v.lastVisitTime, v.lastUpdateTime, v.visitTimes, v.updateCycle)
        case v: TwitterKeywordView => ViewMetaRecord(v.source.name, v.name, None,
                                                     v.startTime, v.lastVisitTime, v.lastUpdateTime, v.visitTimes, v.updateCycle)
      }
    }.map(Json.toJson[ViewMetaRecord](_).toString()).mkString("[ ", " , ", " ]")
    val aql =
      s"""
         |upsert into dataset $ViewMetaDataSetName (
         |$viewsToRecord
         |);
       """.stripMargin
  }

  def keywordViewTemplate(keyword: String): DBQuery = {
    import TwitterDataStore._
    DBQuery(SummaryLevel(SpatialLevels.Point, TimeLevels.TimeStamp), Seq(KeywordPredicate(FieldKeyword, Seq(keyword))))
  }

  def summaryViewTemplate(summaryLevel: SummaryLevel): DBQuery = {
    DBQuery(summaryLevel, Seq.empty)
  }

  def generateSubSetViewAQL(sourceName: String, keyword: String): String = {
    import TwitterDataStore._
    val viewName = s"${sourceName}_${keyword}"
    s"""
       |use dataverse $DataVerse
       |create dataset $viewName($DataType) if not exists primary key "$FieldPrimaryKey";
       |insert into dataset $viewName(
       |for $$t in dataset $sourceName
       |let $$keyword0 := "$keyword"
       |where similarity-jaccard(word-tokens($$t."text"), word-tokens($$keyword0)) > 0.0
       |return $$t)
       """.stripMargin
  }

  def generateSummaryViewAQL(sourceName: String, summaryLevel: SummaryLevel): String = {
    val viewName = s"${sourceName}_"
    import TwitterCountyDaySummaryView._

    //TODO hard coded for (county, day) level now.
    s"""
       |use dataverse $DataVerse
       |create type autoType if not exists as open {
       |  id: uuid
       |}
       |create dataset ${viewName} if not exists primary key id autogenerated;
       |
       |insert into dataset ${viewName}
       |(for $$t in dataset ${sourceName}
       |  group by
       |  $$state := $$t.geo_tag.stateID,
       |  $$county := $$t.geo_tag.countyID,
       |  $$timeBin := interval-bin($$t.create_at, datetime("2012-01-01T00:00:00"), day-time-duration("P1D")) with $$t
       |  return {
       |    "stateID": $$state,
       |    "countyID": $$county,
       |    "timeBin": $$timeBin,
       |    "tweetCount": count($$t),
       |    "retweetCount": count(for $$tt in $$t where $$tt.is_retweet return $$tt),
       |    "users": (for $$tt in $$t group by $$uid := $$tt.user.id with $$tt return $$uid),
       |    "topHashTags": (for $$tt in $$t
       |                      where not(is-null($$tt.hashtags))
       |                      for $$h in $$tt.hashtags
       |                      group by $$tag := $$h with $$h
       |                      let $$c := count($$h)
       |                      order by $$c desc
       |                      limit 50
       |                      return { "tag": $$tag, "count": $$c})
       |  }
       |)
     """.stripMargin

  }
}

case class ViewMetaRecord(sourceName: String,
                          viewKey: String,
                          summaryLevel: Option[SummaryLevel],
                          //                          queryTemplate: DBQuery,
                          startTime: DateTime,
                          lastVisitTime: DateTime,
                          lastUpdateTime: DateTime,
                          visitTimes: Int,
                          updateCycle: Duration
                         )

object ViewMetaRecord {
  val timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val jodaTimeReaders: Reads[DateTime] = Reads.jodaDateReads(timeFormat)
  implicit val jodaTimeWriters: Writes[DateTime] = Writes.jodaDateWrites(timeFormat)

  implicit val summaryLevelFormat: Format[SummaryLevel] = {
    new Format[SummaryLevel] {
      val spatialKey = "spatialLevel"
      val timeKey = "timeLevel"

      override def writes(level: SummaryLevel): JsValue = {
        JsObject(Seq(spatialKey -> JsNumber(level.spatialLevel.id), timeKey -> JsNumber(level.timeLevel.id)))
      }

      override def reads(json: JsValue): JsResult[SummaryLevel] = {
        JsSuccess {
          SummaryLevel(SpatialLevels((json \ spatialKey).as[Int]), TimeLevels((json \ timeKey).as[Int]))
        }
      }
    }
  }

  implicit val durationFormat: Format[Duration] = {
    new Format[Duration] {

      import scala.concurrent.duration._

      override def writes(o: Duration): JsValue = JsNumber(o.toSeconds)

      override def reads(json: JsValue): JsResult[Duration] = JsSuccess(Duration(json.as[Long], SECONDS))
    }
  }

  implicit val queryFormat: Format[DBQuery] = {
    new Format[DBQuery] {
      override def writes(o: DBQuery): JsValue = ???

      override def reads(json: JsValue): JsResult[DBQuery] = ???
    }
  }

  implicit val formatter:Format[ViewMetaRecord] = Json.format[ViewMetaRecord]
}
