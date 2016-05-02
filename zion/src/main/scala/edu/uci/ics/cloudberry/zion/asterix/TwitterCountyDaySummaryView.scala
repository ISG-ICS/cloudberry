package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api._
import org.joda.time.{DateTime, Interval}

import scala.concurrent.Future
import scala.concurrent.duration._

class TwitterCountyDaySummaryView(val conn: AsterixConnection,
                                  val source: TwitterDataStore,
                                  val queryTemplate: DBQuery,
                                  val startTime: DateTime,
                                  var lastVisitTime: DateTime,
                                  var lastUpdateTime: DateTime,
                                  var visitTimes: Int,
                                  val updateCycle: Duration = 1 hours
                                 ) extends AbstractTwitterView with SummaryView {
  override val summaryLevel: SummaryLevel = SummaryLevel(SpatialLevels.County, TimeLevels.Day)

  override val name: String = source.name + "_"

  import TwitterCountyDaySummaryView._

  override def update(query: DBUpdateQuery): Future[Response] = ???

  override def query(query: DBQuery): Future[Response] = {
    if (this.summaryLevel.isFinerThan(query.summaryLevel)) {
      super[AbstractTwitterView].query(query)
    } else {
      source.query(query)
    }
  }

  override protected def usingViewOnly(query: DBQuery): Future[Response] = {
    val aql = generateAQL(query)
    conn.post(aql).map(wsResponse => wsResponse.json.as[SpatialTimeCount])
  }

  //TODO don't care about the predicate so far, always return all the result for simplicity
  def generateAQL(query: DBQuery): String = {
    s"""
       |use dataverse $DataVerse
       |let $$map := (
       |for $$t in dataset $DataSet
       |${byMap(query.summaryLevel.spatialLevel)}
       |)
       |
       |let $$time := (
       |for $$t in dataset $DataSet
       |${byTime(query.summaryLevel.timeLevel)}
       |)
       |
       |let $$hashtag := (
       |for $$t in dataset $DataSet
       |${byHashTag()}
       |)
       |
       |return {"map": $$map, "time": $$time, "hashtag": $$hashtag }
     """.stripMargin
  }

  override protected def createSourceQuery(initQuery: DBQuery, unCovered: Seq[Interval]): DBQuery = {
    import TwitterDataStore._
    val newTimes = TimePredicate(FieldCreateAt, unCovered)
    initQuery.copy(predicates = Seq(newTimes))
  }
}

object TwitterCountyDaySummaryView {
  val DataVerse = "twitter"
  val DataSet = "ds_tweet_"
  val FieldStateID = "stateID"
  val FieldCountyID = "countyID"
  val FieldTimeBin = "timeBin"
  val FieldTweetCount = "tweetCount"
  val FieldReTweetCount = "retweetCount"
  val FieldUserSet = "users"
  val FieldTopHashTag = "topHashTags"

  import SpatialLevels._
  import TimeLevels._

  val SpatialLevelsMap = Map[SpatialLevels.Value, String](State -> FieldStateID, County -> FieldCountyID)
  val TimeFormatMap = Map[TimeLevels.Value, String](Year -> "YYYY", Month -> "YYYY-MM", Day -> "YYYY-MM-DD")

  //TODO move this hacking code to visitor
  private def byMap(level: SpatialLevels.Value): String = {
    s"""
       |group by $$c := $$t.${SpatialLevelsMap.getOrElse(level, FieldStateID)} with $$t
       |return { "key": $$c , "count": sum(for $$x in $$t return $$x.$FieldTweetCount) }
     """.stripMargin
  }

  private def byTime(level: TimeLevels.Value): String = {
    s"""
       |group by $$c := print-datetime(get-interval-start($$t.$FieldTimeBin), "${TimeFormatMap.getOrElse(level, "YYYY-MM-DD")}") with $$t
       |return { "key" : $$c  "count": sum(for $$x in $$t return $$x.$FieldTweetCount)}
    """.stripMargin
  }

  private def byHashTag(): String = {
    s"""
       |for $$h in $$t.$FieldTopHashTag
       |group by $$tag := $$h.tag with $$h
       |let $$c := sum(for $$x in $$h return $$x.count)
       |order by $$c desc
       |limit 50
       |return { "key": $$tag, "count" : $$c}
     """.stripMargin
  }

}
