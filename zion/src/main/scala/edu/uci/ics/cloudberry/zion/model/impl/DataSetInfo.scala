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

  implicit val seqAnyValue: Reads[Seq[Any]] = JSONParser.seqAnyValue

  implicit val intervalFormat: Format[Interval] = new Format[Interval] {
    override def reads(json: JsValue) = {
      val start = (json \ "start").as[DateTime]
      val end = (json \ "end").as[DateTime]
      JsSuccess(new Interval(start.getMillis, end.getMillis))
    }

    override def writes(interval: Interval): JsValue = {
      val formatter = ISODateTimeFormat.dateTime().withZoneUTC()
      JsObject(List("start" -> JsString(interval.getStart.toString(formatter)), "end" -> JsString(interval.getEnd.toString(formatter))))
    }
  }


  implicit val fieldFormat: Format[Field] = new Format[Field] {
    override def reads(json: JsValue): JsResult[Field] = {
      val name = (json \ "name").as[String]
      val isOptional = (json \ "isOptional").as[Boolean]
      DataType.withName((json \ "datatype").as[String]) match {
        case DataType.Number =>
          JsSuccess(NumberField(name, isOptional))
        case DataType.Record =>
          JsSuccess(RecordField(name, ???, isOptional))
        case DataType.Point =>
          JsSuccess(PointField(name, isOptional))
        case DataType.Bag =>
          JsSuccess(BagField(name, ???, isOptional))
        case DataType.Boolean =>
          JsSuccess(BooleanField(name, isOptional))
        case DataType.Hierarchy =>
          JsSuccess(HierarchyField(name, ???, ???))
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
      JsObject(List("name" -> JsString(field.name), "isOptional" -> JsBoolean(field.isOptional), "datatype" -> JsString(field.dataType.toString)))
    }
  }

  implicit val datetimeFormat: Format[DateTime] = new Format[DateTime] {
    override def reads(json: JsValue) = {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
      val datetime = formatter.parseDateTime(json.as[String]);
      JsSuccess(datetime)
    }

    override def writes(dateTime: DateTime): JsValue = {
      val formatter = ISODateTimeFormat.dateTime().withZoneUTC()
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