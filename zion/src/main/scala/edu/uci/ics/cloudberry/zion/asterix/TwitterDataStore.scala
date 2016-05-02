package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api.{DBQuery, Response, SpatialLevels, TimeLevels}
import org.joda.time.Interval
import play.api.libs.json.{Format, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class TwitterDataStore(conn: AsterixConnection) extends AsterixDataStore(TwitterDataStore.Name, conn) {

  import TwitterDataStore._

  // TODO use the Visitor pattern to generate the AQL instead of this hacking code
  override def query(query: DBQuery): Future[Response] = {
    conn.post(generateAQL(query)).map(handleWSResponse)
  }

  def generateAQL(query: DBQuery): String = {
    val aqlVisitor = AQLVisitor(this)
    val predicate = query.predicates.map(p => aqlVisitor.visitPredicate("t", p)).mkString("\n")
    s"""
       |use dataverse $DataVerse
       |let $$common := (
       |for $$t in dataset $DataSet
       |$predicate
       |return $$t
       |)
       |
       |let $$map := (
       |for $$t in $$common
       |${byMap(query.summaryLevel.spatialLevel)}
       |)
       |
       |let $$time := (
       |for $$t in $$common
       |${byTime(query.summaryLevel.timeLevel)}
       |)
       |
       |let $$hashtag := (
       |for $$t in $$common
       |where not(is-null($$t.hashtags))
       |${byHashTag()}
       |)
       |
       |return {"map": $$map, "time": $$time, "hashtag": $$hashtag }
      """.stripMargin
  }

  def handleWSResponse(wsResponse: WSResponse): Response = {
    wsResponse.json.as[SpatialTimeCount]
  }
}

object TwitterDataStore {
  val DataVerse = "twitter"
  val DataSet = "ds_tweet"
  val DataType = "typeTweet"
  val Name = s"$DataVerse.$DataSet"

  val FieldPrimaryKey = "id"
  val FieldStateID = "geo_tag.stateID"
  val FieldCountyID = "geo_tag.countyID"
  val FieldCityID = "geo_tag.cityID"

  val FieldCreateAt = "create_at"
  val FieldKeyword = "text"
  val FieldHashTag = "hash_tag"

  import SpatialLevels._
  import TimeLevels._

  private var twitterDataStore: TwitterDataStore = null

  def apply(conn: AsterixConnection): TwitterDataStore = {
    if (twitterDataStore == null) {
      twitterDataStore = new TwitterDataStore(conn)
    }
    twitterDataStore
  }

  val SpatialLevelMap = Map[SpatialLevels.Value, String](State -> FieldStateID, County -> FieldCountyID, City -> FieldCityID)

  val TimeFormatMap = Map[TimeLevels.Value, String](Year -> "YYYY", Month -> "YYYY-MM", Day -> "YYYY-MM-DD",
                                                    Hour -> "YYYY-MM-DD hh", Minute -> "YYYY-MM-DD hh:mm", Second -> "YYYY-MM-DD hh:mm:ss")

  //TODO move this hacking code to visitor
  private def byMap(level: SpatialLevels.Value): String = {
    s"""
       |group by $$c := $$t.${SpatialLevelMap.getOrElse(level, FieldStateID)} with $$t
       |return { "key": $$c , "count": count($$t) }
     """.stripMargin
  }

  private def byTime(level: TimeLevels.Value): String = {
    s"""
       |group by $$c := print-datetime($$t.create_at, "${TimeFormatMap.getOrElse(level, "YYYY-MM-DD")}") with $$t
       |let $$count := count($$t)
       |return { "key" : $$c  "count": $$count }
    """.stripMargin
  }

  private def byHashTag(): String = {
    s"""
       |for $$h in $$t.hashtags
       |group by $$tag := $$h with $$h
       |let $$c := count($$h)
       |order by $$c desc
       |limit 50
       |return { "key": $$tag, "count" : $$c}
     """.stripMargin
  }

  def getTimeRangeDifference(actual: Interval, expected: Seq[Interval]): Seq[Interval] = {
    if (expected.forall(actual.contains)) {
      Seq.empty[Interval]
    } else {
      // may need update query, but should not need to create query.
      import scala.collection.mutable.ArrayBuffer
      val futureInterval = new Interval(Math.min(actual.getStartMillis, expected.map(_.getStartMillis).reduceLeft(_ min _)),
                                        Math.max(actual.getEndMillis, expected.map(_.getEndMillis).reduceLeft(_ max _)))
      val intervals = ArrayBuffer.empty[Interval]
      if (futureInterval.getStartMillis < actual.getStartMillis) {
        intervals += new Interval(futureInterval.getStartMillis, actual.getStartMillis)
      }
      if (actual.getEndMillis < futureInterval.getEndMillis) {
        intervals += new Interval(actual.getEndMillis, futureInterval.getEndMillis)
      }
      intervals
    }
  }

}

case class KeyCountPair(key: String, count: Int)

case class SpatialTimeCount(map: Seq[KeyCountPair], time: Seq[KeyCountPair], hashtag: Seq[KeyCountPair]) extends Response

object SpatialTimeCount {
  implicit val keyCountFormatter: Format[KeyCountPair] = Json.format[KeyCountPair]
  implicit val twitterFormatter: Format[SpatialTimeCount] = Json.format[SpatialTimeCount]
}
