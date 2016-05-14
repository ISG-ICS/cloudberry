package models

import db.Migration_20160324
import edu.uci.ics.cloudberry.util.Rectangle
import org.joda.time.{DateTime, Interval}
import play.api.libs.json._

//TODO add the aggregation requirement parameters. Currently we calculate all the registered aggregation functions.
case class UserQuery(dataset: String,
                     keyword: Option[String],
                     timeRange: Interval,
                     area: Rectangle,
                     level: String,
                     repeatDuration: Long = 0
                    )

object UserQuery {

  val Sample = UserQuery(Migration_20160324.TweetDataSet,
                         Some("rain"),
                         new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis(), new DateTime(2016, 3, 1, 0, 0).getMillis()),
                         Rectangle(-146.95312499999997, 7.798078531355303, -45.703125, 61.3546135846894),
                         level = "state")

  implicit val intervalFormat: Format[Interval] = {
    new Format[Interval] {
      override def writes(interval: Interval): JsValue = {
        JsObject(Seq(("start", JsNumber(interval.getStartMillis)), ("end", JsNumber(interval.getEndMillis))))
      }

      override def reads(json: JsValue): JsResult[Interval] = {
        JsSuccess(new Interval((json \ "start").as[Long], (json \ "end").as[Long]))
      }
    }
  }

  implicit val userQueryFormat: Format[UserQuery] = Json.format[UserQuery]
}