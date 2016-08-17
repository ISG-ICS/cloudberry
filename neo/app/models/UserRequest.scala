package models

import org.joda.time.Interval
import play.api.libs.json._


object GeoLevel extends Enumeration {
  type Level = Value
  val State = Value("state")
  val County = Value("county")
  val City = Value("city")
}

object TimeBin extends Enumeration {
  type Bin = Value
  val Day = Value("day")
  val Hour = Value("hour")
}

//TODO support more queries
case class UserRequest(dataset: String,
                       keywords: Seq[String],
                       timeInterval: Interval,
                       timeBin: TimeBin.Bin,
                       geoLevel: GeoLevel.Level,
                       geoIds: Seq[Int]
                      )

object UserRequest {

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

  def enumerationReader[E <: Enumeration](enum: E) = new Reads[enum.Value] {
    override def reads(json: JsValue): JsResult[enum.Value] = {
      val key = json.as[String]
      enum.values.find(_.toString == key) match {
        case Some(value) => JsSuccess(value)
        case None => JsError(s"$key not found in enum: $enum")
      }
    }
  }

  implicit val geoLevelReader: Reads[GeoLevel.Level] = enumerationReader(GeoLevel)

  implicit val timeBinReader: Reads[TimeBin.Bin] = enumerationReader(TimeBin)

  implicit val userQueryReader: Reads[UserRequest] = Json.reads[UserRequest]
}

