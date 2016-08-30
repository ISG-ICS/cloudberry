package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.JsonRequestException
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Interval}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Stats(createTime: DateTime,
                 lastModifyTime: DateTime,
                 lastReadTime: DateTime,
                 cardinality: Long)

case class DataSetInfo(name: String,
                       createQueryOpt: Option[Query],
                       schema: Schema,
                       dataInterval: Interval,
                       stats: Stats)

object DataSetInfo {

  val MetaSchema: Schema = Schema("berry.MetaType",
                                  Seq(StringField("name")),
                                  Seq.empty,
                                  Seq("name"),
                                  "stats.createTime")

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
      JsObject(List("start" -> Json.toJson(interval.getStart), "end" -> Json.toJson(interval.getEnd)))
    }
  }

  //Used by: HierarchyField: "levels" -> Json.toJson(hierarchy.levels))) to write from Seq[(String,String)] to JSON
  implicit def tuple2Writes: Writes[(String, String)] = Writes[(String, String)](t => Json.obj("level" -> t._1, "field" -> t._2))

  def parseLevels(levelSeq: Seq[Map[String, String]]): Seq[(String, String)] = {
    levelSeq.map {
      levelMap => (levelMap("level"), levelMap("field"))
    }
  }

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
          val levelSeq = (json \ "levels").as[Seq[Map[String, String]]]
          JsSuccess(HierarchyField(name, DataType.withName(innerType), parseLevels(levelSeq)))
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
        case bag: BagField => JsObject(List(
          "name" -> JsString(name),
          "isOptional" -> JsBoolean(isOptional),
          "datatype" -> JsString(dataType),
          "innerType" -> JsString(bag.innerType.toString)))
        case hierarchy: HierarchyField => JsObject(List(
          "name" -> JsString(name),
          "isOptional" -> JsBoolean(isOptional),
          "datatype" -> JsString(dataType),
          "innerType" -> JsString(hierarchy.innerType.toString),
          "levels" -> Json.toJson(hierarchy.levels)))
        case basicField: Field => JsObject(List(
          "name" -> JsString(name),
          "isOptional" -> JsBoolean(isOptional),
          "datatype" -> JsString(dataType)))
      }

    }
  }

  implicit val datetimeFormat: Format[DateTime] = new Format[DateTime] {
    override def reads(json: JsValue) = {
      val datetime = TimeField.TimeFormat.parseDateTime(json.as[String])
      JsSuccess(datetime)
    }

    override def writes(dateTime: DateTime): JsValue = JsString(dateTime.toString(TimeField.TimeFormat))

  }

  implicit val statsFormat: Format[Stats] = (
    (JsPath \ "createTime").format[DateTime] and
      (JsPath \ "lastModifyTime").format[DateTime] and
      (JsPath \ "lastReadTime").format[DateTime] and
      (JsPath \ "cardinality").format[Long]
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