package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{FieldNotFound, JsonRequestException, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import edu.uci.ics.cloudberry.zion.model.impl.Unresolved._
import org.joda.time.{DateTime, Interval}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import scala.collection

case class Stats(createTime: DateTime,
                 lastModifyTime: DateTime,
                 lastReadTime: DateTime,
                 cardinality: Long)

case class DataSetInfo(name: String,
                       createQueryOpt: Option[Query],
                       schema: AbstractSchema,
                       dataInterval: Interval,
                       stats: Stats)

object DataSetInfo {

  val MetaDataDBName: String = "berry.meta"
  val MetaSchema = Schema(
    "berry.MetaType",
    Seq(StringField("name"), TimeField("stats.createTime")),
    Seq.empty,
    Seq(StringField("name")),
    TimeField("stats.createTime")
  )

  /**
    * Parse a json object to create a [[DataSetInfo]].
    * First parses the json object into a [[UnresolvedDataSetInfo]], and then resolves it into a [[DataSetInfo]]
    * @param json
    * @param schemaMap
    * @return
    */
  def parse(json: JsValue, schemaMap: Map[String, AbstractSchema]): DataSetInfo = {
    json.validate[UnresolvedDataSetInfo] match {
      case js: JsSuccess[UnresolvedDataSetInfo] =>
        val dataSetInfo = js.get
        val resolvedQuery = dataSetInfo.createQueryOpt.map(JSONParser.resolve(_, schemaMap))
        val resolvedSchema = dataSetInfo.schema.toResolved

        DataSetInfo(dataSetInfo.name, resolvedQuery, resolvedSchema, dataSetInfo.dataInterval, dataSetInfo.stats)

      case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
    }
  }

  /**
    * Write a dataSetInfo as a json object.
    * Calls [[Unresolved.toUnresolved()]] to transform the dataSetInfo into [[UnresolvedDataSetInfo]],
    * which is then written as a json object.
    * @param dataSetInfo
    * @return
    */
  def write(dataSetInfo: DataSetInfo): JsValue =
    Json.toJson(toUnresolved(dataSetInfo))

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

  implicit val queryFormat: Format[UnresolvedQuery] = JSONParser.queryFormat

  implicit val statsFormat: Format[Stats] = (
    (JsPath \ "createTime").format[DateTime] and
      (JsPath \ "lastModifyTime").format[DateTime] and
      (JsPath \ "lastReadTime").format[DateTime] and
      (JsPath \ "cardinality").format[Long]
    ) (Stats.apply, unlift(Stats.unapply))

  implicit val schemaFormat: Format[UnresolvedSchema] = (
    (JsPath \ "typeName").format[String] and
      (JsPath \ "dimension").format[Seq[Field]] and
      (JsPath \ "measurement").format[Seq[Field]] and
      (JsPath \ "primaryKey").format[Seq[String]] and
      (JsPath \ "timeField").formatNullable[String]
    ) (UnresolvedSchema.apply, unlift(UnresolvedSchema.unapply))

  implicit val dataSetInfoFormat: Format[UnresolvedDataSetInfo] = (
    (JsPath \ "name").format[String] and
      (JsPath \ "createQuery").formatNullable[UnresolvedQuery] and
      (JsPath \ "schema").format[UnresolvedSchema] and
      (JsPath \ "dataInterval").format[Interval] and
      (JsPath \ "stats").format[Stats]
    ) (UnresolvedDataSetInfo.apply, unlift(UnresolvedDataSetInfo.unapply))
}
