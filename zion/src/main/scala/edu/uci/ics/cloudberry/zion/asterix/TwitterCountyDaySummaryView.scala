package edu.uci.ics.cloudberry.zion.asterix

import akka.actor.ActorRef
import edu.uci.ics.cloudberry.zion.actor.{ViewActor, ViewMetaRecord}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class TwitterCountyDaySummaryView(val conn: AsterixConnection,
                                  val queryTemplate: DBQuery,
                                  override val sourceActor: ActorRef,
                                  fViewStore: Future[ViewMetaRecord],
                                  config: Config
                                 )(implicit ec: ExecutionContext) extends ViewActor(sourceActor, fViewStore, config) {

  import TwitterCountyDaySummaryView._

  override def mergeResult(viewResponse: Response, sourceResponse: Response): Response = {
    val viewCount = viewResponse.asInstanceOf[SpatialTimeCount]
    val sourceCount = sourceResponse.asInstanceOf[SpatialTimeCount]
    TwitterDataStoreActor.mergeResult(viewCount, sourceCount)
  }

  override def createSourceQuery(initQuery: DBQuery, unCovered: Seq[Interval]): DBQuery = {
    import TwitterDataStoreActor._
    val newTimes = TimePredicate(FieldCreateAt, unCovered)
    new DBQuery(initQuery.summaryLevel, Seq(newTimes))
  }

  override def updateView(from: DateTime, to: DateTime): Future[Unit] = {
    val aql = TwitterViewsManagerActor.generateSummaryUpdateAQL(sourceName, key, summaryLevel, from, to)
    conn.postUpdate(aql).map[Unit] { succeed: Boolean =>
      if (!succeed) {
        throw UpdateFailedDBException(key)
      }
    }
  }

  override def askViewOnly(query: DBQuery): Future[Response] = {
    askAsterixAndGetAllResponse(conn, key, query)
  }

  override def updateInterval: FiniteDuration = config.ViewUpdateInterval
}


object TwitterCountyDaySummaryView {
  val DataVerse = "twitter"
  val FieldStateID = "stateID"
  val FieldCountyID = "countyID"
  val FieldTimeBin = "timeBin"
  val FieldTweetCount = "tweetCount"
  val FieldReTweetCount = "retweetCount"
  val FieldUserSet = "users"
  val FieldTopHashTag = "topHashTags"
  val SummaryLevel = new SummaryLevel(SpatialLevels.County, TimeLevels.Day)

  import SpatialLevels._
  import TimeLevels._

  val SpatialLevelsMap = Map[SpatialLevels.Value, String](State -> FieldStateID, County -> FieldCountyID)
  val TimeFormatMap = Map[TimeLevels.Value, String](Year -> "YYYY", Month -> "YYYY-MM", Day -> "YYYY-MM-DD")

  //TODO temporary solution
  def visitPredicate(variable: String, summaryLevel: SummaryLevel, predicate: Predicate): String = {
    import AQLVisitor.TimeFormat
    val spID = SpatialLevelsMap.get(summaryLevel.spatialLevel).get
    predicate match {
      case p: KeywordPredicate => ""
      case p: TimePredicate =>
        def formatInterval(interval: Interval): String = {
          s"""
             |(get-interval-start($$$variable.$FieldTimeBin) >= datetime("${TimeFormat.print(interval.getStart)}")
             |and get-interval-start($$$variable.$FieldTimeBin) < datetime("${TimeFormat.print(interval.getEnd)}"))
             |""".stripMargin
        }
        s"""
           |where
           |${p.intervals.map(formatInterval).mkString("or")}
           |""".stripMargin
      case p: IdSetPredicate =>
        s"""
           |for $$sid in [ ${p.idSets.mkString(",")} ]
           |where $$$variable.$spID = $$sid
           |""".stripMargin
    }

  }

  def askAsterixAndGetAllResponse(conn: AsterixConnection, name: String, query: DBQuery)(implicit ec: ExecutionContext): Future[Response] = {
    import TwitterDataStoreActor.handleKeyCountResponse
    val fMap = conn.postQuery(generateByMapAQL(name, query)).map(handleKeyCountResponse)
    val fTime = conn.postQuery(generateByTimeAQL(name, query)).map(handleKeyCountResponse)
    val fHashtag = conn.postQuery(generateByHashtagAQL(name, query)).map(handleKeyCountResponse)
    for {
      mapResult <- fMap
      timeResult <- fTime
      hashtagResult <- fHashtag
    } yield SpatialTimeCount(mapResult, timeResult, hashtagResult)
  }

  def generateByMapAQL(dataSet: String, query: DBQuery): String = {
    val common = applyPredicate(dataSet, query)
    s"""|$common
        |let $$map := (
        |for $$t in $$common
        |${byMap(query.summaryLevel.spatialLevel)}
        |)
        |return $$map;
        |""".stripMargin
  }

  def generateByTimeAQL(dataSet: String, query: DBQuery): String = {
    val common = applyPredicate(dataSet, query)
    s"""|$common
        |let $$time := (
        |for $$t in $$common
        |${byTime(query.summaryLevel.timeLevel)}
        |)
        |return $$time
        |""".stripMargin
  }

  def generateByHashtagAQL(dataSet: String, query: DBQuery): String = {
    val common = applyPredicate(dataSet, query)
    s"""|$common
        |let $$hashtag := (
        |for $$t in $$common
        |${byHashTag()}
        |)
        |return $$hashtag
        |""".stripMargin
  }

  private def applyPredicate(dataSet: String, query: DBQuery): String = {
    val predicate = query.predicates.map(visitPredicate("t", query.summaryLevel, _)).mkString("\n")
    s"""
       |use dataverse $DataVerse
       |let $$common := (
       |for $$t in dataset $dataSet
       |$predicate
       |return $$t
       |)
       |""".stripMargin
  }

  //TODO move this hacking code to visitor
  private def byMap(level: SpatialLevels.Value): String = {
    s"""
       |group by $$c := $$t.${SpatialLevelsMap.getOrElse(level, FieldStateID)} with $$t
       |return { "key": string($$c) , "count": sum(for $$x in $$t return $$x.$FieldTweetCount) }
       |""".stripMargin
  }

  private def byTime(level: TimeLevels.Value): String = {
    s"""
       |group by $$c := print-datetime(get-interval-start($$t.$FieldTimeBin), "${TimeFormatMap.getOrElse(level, "YYYY-MM-DD")}") with $$t
       |return { "key" : $$c, "count": sum(for $$x in $$t return $$x.$FieldTweetCount)}
       |""".stripMargin
  }

  private def byHashTag(): String = {
    s"""
       |for $$h in $$t.$FieldTopHashTag
       |group by $$tag := $$h.tag with $$h
       |let $$c := sum(for $$x in $$h return $$x.count)
       |order by $$c desc
       |limit 50
       |return { "key": $$tag, "count" : $$c}
       |""".stripMargin
  }

}
