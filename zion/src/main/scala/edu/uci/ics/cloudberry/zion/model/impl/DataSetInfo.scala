package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.JsonRequestException
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.joda.time.{DateTime, Interval}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Stats(createTime: DateTime,
                 lastModifyTime: DateTime,
                 lastReadTime: DateTime,
                 cardinality: Int)

case class DataSetInfo(name: String,
                       createQueryOpt: Option[Query],
                       schema: Schema,
                       dataInterval: Interval,
                       stats: Stats)

object DataSetInfo {

  def parse(json: JsValue): DataSetInfo = {
    json.validate[DataSetInfo] match {
      case js: JsSuccess[DataSetInfo] => js.get
      case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
    }
  }

  def write(dataSetInfo: DataSetInfo): JsValue = Json.toJson(dataSetInfo)

  implicit val intervalFormat: Format[Interval] = new Format[Interval] {
    override def reads(json: JsValue) = {
      val start = (json \ "start").as[DateTime]
      val end = (json \ "end").as[DateTime]
      JsSuccess(new Interval(start.getMillis, end.getMillis))
    }

    override def writes(interval: Interval): JsValue = {
      val formatter = ISODateTimeFormat.dateTime()
      JsObject(List("start" -> JsString(interval.getStart.toString(formatter)), "end" -> JsString(interval.getEnd.toString(formatter))))
    }
  }

  //Needed for HierarchyField levels
  implicit def tuple2Reads[A, B](implicit aReads: Reads[A], bReads: Reads[B]): Reads[Tuple2[A, B]] = Reads[Tuple2[A, B]] {
    case JsArray(arr) if arr.size == 2 => for {
      a <- aReads.reads(arr(0))
      b <- bReads.reads(arr(1))
    } yield (a, b)
    case _ => JsError("Expected array of two elements")
  }

  implicit def tuple2Writes[A: Writes, B: Writes] = Writes[(A, B)](t => Json.obj("something1" -> t._1, "something1" -> t._2))

  implicit val fieldFormat: Format[Field] = new Format[Field] {
    override def reads(json: JsValue): JsResult[Field] = {
      val name = (json \ "name").as[String]
      val isOptional = (json \ "isOptional").as[Boolean]
      DataType.withName((json \ "datatype").as[String]) match {
        case DataType.Number =>
          JsSuccess(NumberField(name, isOptional))
        case DataType.Record => ???
          //TODO think about Record type later
        case DataType.Point =>
          JsSuccess(PointField(name, isOptional))
        case DataType.Bag =>
          val innerType = (json \ "innerType").as[String]
          JsSuccess(BagField(name, DataType.withName(innerType), isOptional))
        case DataType.Boolean =>
          JsSuccess(BooleanField(name, isOptional))
        case DataType.Hierarchy =>
          val innerType = (json \ "innerType").as[String]
          val levels = (json \ "levels").as[Seq[(String, String)]]
          JsSuccess(HierarchyField(name, DataType.withName(innerType), levels))
        case DataType.Text =>
          JsSuccess(TextField(name, isOptional))
        case DataType.String =>
          JsSuccess(StringField(name, isOptional))
        case DataType.Time =>
          JsSuccess(TimeField(name, isOptional))
        case unknown: DataType.Value => JsError(s"field datatype invalid: $unknown")
      }
    }

    override def writes(field: Field): JsValue = {
      val name = field.name
      val isOptional = field.isOptional
      val dataType = field.dataType.toString
      field match {
        case record: RecordField => JsNull
        case bag: BagField => JsObject(List("name" -> JsString(name), "isOptional" -> JsBoolean(isOptional), "datatype" -> JsString(dataType), "innerType" -> JsString(bag.innerType.toString)))
        case hierarchy: HierarchyField => JsObject(List("name" -> JsString(name), "isOptional" -> JsBoolean(isOptional), "datatype" -> JsString(dataType), "innerType" -> JsString(hierarchy.innerType.toString)))
        case basicField: Field => JsObject(List("name" -> JsString(name), "isOptional" -> JsBoolean(isOptional), "datatype" -> JsString(dataType)))
        }
    }
  }

  implicit val datetimeFormat: Format[DateTime] = new Format[DateTime] {
    override def reads(json: JsValue) = {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
      val datetime = formatter.parseDateTime(json.as[String])
      JsSuccess(datetime)
    }

    override def writes(dateTime: DateTime): JsValue = {
      val formatter = ISODateTimeFormat.dateTime()
      JsString(dateTime.toString(formatter))
    }
  }

  implicit val statsFormat: Format[Stats] = (
    (JsPath \ "createTime").format[DateTime] and
      (JsPath \ "lastModifyTime").format[DateTime] and
      (JsPath \ "lastReadTime").format[DateTime] and
      (JsPath \ "cardinality").format[Int]
    ) (Stats.apply, unlift(Stats.unapply))

  implicit val queryFormat: Format[Query] = JSONParser.queryFormat

  implicit val schemaFormat: Format[Schema] = (
    (JsPath \ "typeName").format[String] and
      (JsPath \ "dimension").format[Seq[Field]] and
      (JsPath \ "measurement").format[Seq[Field]] and
      (JsPath \ "primaryKey").format[Seq[String]] and
      (JsPath \ "timeField").format[String]
    ) (Schema.apply, unlift(Schema.unapply))

  implicit val dataSetInfoFormat: Format[DataSetInfo] = (
    (JsPath \ "name").format[String] and
      (JsPath \ "createQuery").formatNullable[Query] and
      (JsPath \ "schema").format[Schema] and
      (JsPath \ "dataInterval").format[Interval] and
      (JsPath \ "stats").format[Stats]
    ) (DataSetInfo.apply, unlift(DataSetInfo.unapply))
}