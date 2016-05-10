package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.{Actor, ActorRef, Props}
import edu.uci.ics.cloudberry.zion.actor.{ViewMetaRecord, ViewsManagerActor}
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class TwitterViewsManagerActor(val conn: AsterixConnection, override val sourceActor: ActorRef
                              )(implicit ec: ExecutionContext) extends ViewsManagerActor(TwitterDataStoreActor.Name, sourceActor) {

  import TwitterViewsManagerActor._

  override def getViewKey(query: DBQuery): String = {
    val keyword = query.predicates.find(_.isInstanceOf[KeywordPredicate]).map(_.asInstanceOf[KeywordPredicate].keywords.head).getOrElse("")
    sourceName + "_" + keyword
  }

  override def flushInterval: FiniteDuration = 1 hours

  override def createViewActor(key: String, query: DBQuery, fView: Future[ViewMetaRecord]): ActorRef = {
    query.predicates.find(_.isInstanceOf[KeywordPredicate]).map(_.asInstanceOf[KeywordPredicate]) match {
      case Some(x) =>
        context.actorOf(Props(classOf[TwitterKeywordViewActor],
                              conn, keywordViewTemplate(x.keywords.head), x.keywords.head, sourceActor, fView, ec),
                        key)
      case None =>
        context.actorOf(Props(classOf[TwitterCountyDaySummaryView],
                              conn, summaryViewTemplate(TwitterCountyDaySummaryView.SummaryLevel), sourceActor, fView, ec),
                        key)
    }
  }

  override def createViewStore(query: DBQuery): Future[ViewMetaRecord] = {
    val key = getViewKey(query)
    val optKeyword = query.predicates.find(_.isInstanceOf[KeywordPredicate]).map(_.asInstanceOf[KeywordPredicate])
    if (optKeyword.isDefined) {
      val keyword = optKeyword.get.keywords.head
      conn.post(generateSubSetViewAQL(sourceName, keyword)).map { ws =>
        if (ws.status != 200) throw new IllegalStateException(ws.body)
        ViewMetaRecord(sourceName, key, SummaryLevel(SpatialLevels.Point, TimeLevels.TimeStamp),
                       startTime = new DateTime(0l), lastVisitTime = new DateTime(), lastUpdateTime = new DateTime(),
                       visitTimes = 0, updateCycle = flushInterval)
      }
    } else {
      conn.post(generateSummaryViewAQL(sourceName, query.summaryLevel)).map { ws =>
        if (ws.status != 200) throw new IllegalStateException(ws.body)
        ViewMetaRecord(sourceName, key, SummaryLevel(SpatialLevels.County, TimeLevels.Day),
                       startTime = new DateTime(0l), lastVisitTime = new DateTime(), lastUpdateTime = new DateTime(),
                       visitTimes = 0, updateCycle = flushInterval)
      }
    }
  }

  override def loadMetaStore: Future[Seq[ViewMetaRecord]] = loadFromMetaStore(sourceName, conn)

  override def flushMeta(): Unit = flushMetaToStore(conn, viewMeta.values.toSeq)
}

object TwitterViewsManagerActor {

  val ViewMetaDataSetName = "twitter.viewMeta"
  val ViewMetaCreateAQL =
    s"""
       |use dataverse ${TwitterDataStoreActor.DataVerse}
       |create type typeViewMeta2 if not exists as open {
       |  sourceName: string,
       |  viewKey: string
       |}
       |create dataset $ViewMetaDataSetName (typeViewMeta2) if not exists primary key viewKey
       |""".stripMargin

  def loadFromMetaStore(source: String, conn: AsterixConnection)(implicit ec: ExecutionContext): Future[Seq[ViewMetaRecord]] = {
    val aql =
      s"""
         |use dataverse ${TwitterDataStoreActor.DataVerse}
         |for $$t in dataset $ViewMetaDataSetName
         |where $$t.sourceName = "$source"
         |return $$t
       """.stripMargin
    conn.post(aql).map { ws =>
      if (ws.status == 200) {
        ws.json.as[Seq[ViewMetaRecord]]
      } else {
        Seq.empty[ViewMetaRecord]
      }
    }
  }

  def flushMetaToStore(conn: AsterixConnection, seq: Seq[ViewMetaRecord]): Future[WSResponse] = {
    val viewRecords = Json.prettyPrint(Json.toJson[Seq[ViewMetaRecord]](seq))
    val aql =
      s"""
         |$ViewMetaCreateAQL
         |upsert into dataset $ViewMetaDataSetName (
         |$viewRecords
         |);
       """.stripMargin
    conn.post(aql)
  }

  def keywordViewTemplate(keyword: String): DBQuery = {
    import TwitterDataStoreActor._
    DBQuery(SummaryLevel(SpatialLevels.Point, TimeLevels.TimeStamp), Seq(KeywordPredicate(FieldKeyword, Seq(keyword))))
  }

  def summaryViewTemplate(summaryLevel: SummaryLevel): DBQuery = {
    DBQuery(summaryLevel, Seq.empty)
  }

  def generateSubSetViewAQL(sourceName: String, keyword: String): String = {
    import TwitterDataStoreActor._
    val viewName = s"${sourceName}_${keyword}"
    s"""
       |use dataverse $DataVerse
       |drop dataset $viewName if exists
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
       |drop dataset $viewName if exists
       |
       |create type autoType if not exists as open {
       |  id: uuid
       |}
       |create dataset ${viewName}(autoType) if not exists primary key id autogenerated;
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
       |    "users": count(for $$tt in $$t group by $$uid := $$tt.user.id with $$tt return $$uid),
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


