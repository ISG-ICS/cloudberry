package edu.uci.ics.cloudberry.zion.actor

import edu.uci.ics.cloudberry.zion.model.{SpatialLevels, SummaryLevel, TimeLevels}
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ViewMetaRecordTest extends Specification {

  import scala.concurrent.duration._

  DateTimeZone.setDefault(DateTimeZone.UTC)
  val sampleRecord = ViewMetaRecord("twitter", "twitter_rain", SummaryLevel(SpatialLevels.State, TimeLevels.Day),
                                    startTime = new DateTime(0), lastVisitTime = new DateTime(), lastUpdateTime = new DateTime(),
                                    visitTimes = 10, updateCycle = 1 hours)
  "ViewMetaRecordTest" should {
    "formatter a record" in {
      val jsonString = Json.toJson(sampleRecord).toString()
      val record2 = Json.parse(jsonString).as[ViewMetaRecord]
      sampleRecord must beEqualTo(record2)
    }
  }
}
