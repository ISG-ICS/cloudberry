package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{FieldNotFound, IJSONParser, JsonRequestException, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo.parseLevels
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, _}


class JSONParser extends IJSONParser {

  import JSONParser._

  override def getDatasets(json: JsValue): Set[String] = {
    val datasets = (json \\ "dataset").filter(_.isInstanceOf[JsString]).map(_.asInstanceOf[JsString].value)
    datasets.toSet
  }

  /**
    * Parse a [[JsValue]] with a schema map, and returns a [[Query]]
    * First parse the [[JsValue]] into a [[UnresolvedQuery]] using json parsing APIs provided by Play framework.
    * Then, calls [[QueryResolver]] to resolve it into a [[Query]], which has all fields resolved typed.
    * Finally, calls [[QueryValidator]] to validate the correctness of this query.
    */
  override def parse(originJson: JsValue, schemaMap: Map[String, AbstractSchema]): (Seq[Query], QueryExeOption) = {
    val originOption = (originJson \ "option").toOption.map(_.as[QueryExeOption]).getOrElse(QueryExeOption.NoSliceNoContinue)
    val (json, option) = parseLimit(originJson, originOption)
    val query = (json \ "batch").toOption match {
      case Some(groupRequest) => groupRequest.validate[Seq[UnresolvedQuery]] match {
        case js: JsSuccess[Seq[UnresolvedQuery]] => js.get
        case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
      }
      case None => json.validate[UnresolvedQuery] match {
        case js: JsSuccess[UnresolvedQuery] => Seq(js.get)
        case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
      }
    }
    val resolvedQuery = query.map(resolve(_, schemaMap))
    (resolvedQuery, option)
  }
}

object JSONParser {
  //Warn: the order of implicit values matters. The dependence should be initialized earlier

  /**
    * Resolves a [[UnresolvedQuery]] into a [[Query]] by calling [[QueryResolver]], then validates its correctness by calling [[QueryValidator]].
    *
    * @param query
    * @param schemaMap
    * @return
    */
  def resolve(query: UnresolvedQuery, schemaMap: Map[String, AbstractSchema]): Query = {
    val resolved = QueryResolver.resolve(query, schemaMap).asInstanceOf[Query]
    QueryValidator.validate(resolved, schemaMap)
    resolved
  }

  def parseLimit(json: JsValue, option: QueryExeOption): (JsValue, QueryExeOption) = {
    if (option.sliceMills <= 0) { // non-slicing query
      return (json, QueryExeOption(option.sliceMills, option.continueSeconds, None))
    }
    if ((json \ "batch").toOption.isEmpty && (json \\ "limit").isEmpty) { // single slicing query without limit
      return (json, QueryExeOption(option.sliceMills, option.continueSeconds, None))
    }
    if ((json \ "batch").toOption.isEmpty && (json \\ "limit").nonEmpty) { // single slicing query with limit
      val limit = (json \ "select" \ "limit").as[Int]
      val updatedSelectJson = (json \ "select").as[JsObject] ++ Json.obj("limit" -> Int.MaxValue)
      val updatedJson = json.as[JsObject] ++ Json.obj("select" -> updatedSelectJson)
      return (updatedJson, QueryExeOption(option.sliceMills, option.continueSeconds, Some(limit)))
    }
    if ((json \\ "limit").isEmpty || ((json \\ "limit").nonEmpty && (json \\ "aggregate").nonEmpty)) {
      (json, QueryExeOption(option.sliceMills, option.continueSeconds, None))
    } else {
      // TODO send error messages to user
      throw JsonRequestException("Batch Requests cannot contain \"limit\" field")
    }
  }

  implicit val seqAnyValue: Format[Seq[Any]] = new Format[Seq[Any]] {
    override def reads(json: JsValue): JsResult[Seq[Any]] = {
      json.asOpt[JsArray] match {
        case Some(array) =>
          JsSuccess {
            var (allInt, allLong) = (0, 0)
            val ret = array.value.map {
              case jsBoolean: JsBoolean => jsBoolean.value
              case jsNumber: JsNumber =>
                if (jsNumber.value.isValidInt) {
                  allInt += 1
                  jsNumber.value.toIntExact
                } else if (jsNumber.value.isValidLong) {
                  allLong += 1
                  jsNumber.value.toLongExact
                } else {
                  jsNumber.value.toDouble
                }
              case jsString: JsString => jsString.value
              case other: JsValue => throw JsonRequestException(s"unknown data type: $other")
            }.toList
            //Scala upgrade the type to Double for the mixing case. Here we downgrade it to Int or Long.
            if (ret.size == allInt) ret.map(_.asInstanceOf[Double].toInt)
            else if (ret.size == allLong) ret.map(_.asInstanceOf[Double].toLong)
            else ret
          }
        case None => JsSuccess(Seq.empty)
      }
    }

    override def writes(seq: Seq[Any]): JsValue = {
      JsArray(seq.map {
        case b: Boolean => JsBoolean(b)
        case s: String => JsString(s)
        case i: Int => JsNumber(i)
        case d: Double => JsNumber(d)
        case l: Long => JsNumber(l)
        case fs: UnresolvedFilterStatement => filterFormat.writes(fs)
        case by: UnresolvedByStatement => byFormat.writes(by)
        case ags: UnresolvedAggregateStatement => aggFormat.writes(ags)
        case unS: UnresolvedUnnestStatement => unnestFormat.writes(unS)
        case other: JsValue => throw JsonRequestException(s"unknown data type: $other")
      })
    }
  }

  implicit val transformFuncFormat: Format[TransformFunc] = new Format[TransformFunc] {
    override def reads(json: JsValue): JsResult[TransformFunc] = ???

    override def writes(transformFunc: TransformFunc): JsValue = ???
  }

  implicit val relationFormat: Format[Relation] = new Format[Relation] {
    override def reads(json: JsValue): JsResult[Relation] = {
      try {
        JsSuccess(Relation.withName(json.as[String]))
      } catch {
        case e: NoSuchElementException => JsError(s"unknown relation: $json")
      }
    }

    override def writes(relation: Relation): JsValue = JsString(relation.toString)
  }

  implicit val groupFuncFormat: Format[GroupFunc] = new Format[GroupFunc] {
    override def reads(json: JsValue): JsResult[GroupFunc] = (json \ "name").as[String] match {
      case GroupFunc.Bin =>
        val scale = (json \ "args" \ "scale").as[Int]
        JsSuccess(Bin(scale))
      case GroupFunc.Level =>
        val level = (json \ "args" \ "level").as[String]
        JsSuccess(Level(level))
      case GroupFunc.Interval =>
        try {
          val unit = TimeUnit.withName((json \ "args" \ "unit").as[String])
          val x = (json \ "args" \ "x").asOpt[Int].getOrElse(1)
          JsSuccess(Interval(unit, x))
        } catch {
          case e: NoSuchElementException => JsError(s"unknown time unit ${
            e.getMessage
          } ")
        }
      case GroupFunc.GeoCellTenth => JsSuccess(GeoCellTenth)
      case GroupFunc.GeoCellHundredth => JsSuccess(GeoCellHundredth)
      case GroupFunc.GeoCellThousandth => JsSuccess(GeoCellThousandth)
      case unknown: String => JsError(s"group function not found: $unknown")
    }

    override def writes(groupFunc: GroupFunc): JsValue = {
      groupFunc match {
        case fBin: Bin => JsObject(Seq("name" -> JsString(fBin.name), "args" -> JsObject(Seq("scale" -> JsNumber(fBin.scale)))))
        case fLevel: Level => JsObject(Seq("name" -> JsString(fLevel.name), "args" -> JsObject(Seq("level" -> JsString(fLevel.levelTag)))))
        case fInterval: Interval => JsObject(Seq("name" -> JsString(fInterval.name), "args" -> JsObject(Seq("unit" -> JsString(fInterval.unit.toString)))))
        case _: GeoCellScale => JsObject(Seq("name" -> JsString(groupFunc.name)))
      }
    }
  }

  implicit val aggFuncFormat: Format[AggregateFunc] = new Format[AggregateFunc] {
    override def reads(json: JsValue): JsResult[AggregateFunc] = {
      (json \ "name").as[String] match {
        case AggregateFunc.Count => JsSuccess(Count)
        case AggregateFunc.TopK => ???
        case AggregateFunc.Sum => JsSuccess(Sum)
        case AggregateFunc.Max => JsSuccess(Max)
        case AggregateFunc.Min => JsSuccess(Min)
        case AggregateFunc.Avg => JsSuccess(Avg)
        case AggregateFunc.DistinctCount => ???
        case unknown: String => JsError(s"unknown aggregation function: $unknown")
      }
    }

    override def writes(aggregateFunc: AggregateFunc): JsValue = {
      JsObject(List("name" -> JsString(aggregateFunc.name)))
    }
  }

  implicit val aggFormat: Format[UnresolvedAggregateStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "apply").format[AggregateFunc] and
      (JsPath \ "as").format[String]
    ) (UnresolvedAggregateStatement.apply, unlift(UnresolvedAggregateStatement.unapply))

  implicit val byFormat: Format[UnresolvedByStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "apply").formatNullable[GroupFunc] and
      (JsPath \ "as").formatNullable[String]
    ) (UnresolvedByStatement.apply, unlift(UnresolvedByStatement.unapply))


  implicit val typeFormat: Format[DataType.DataType] = new Format[DataType.DataType] {
    override def reads(json: JsValue): JsResult[DataType.DataType] = {
      val fieldType = json.as[String]
      DataType.values.find(_.toString == fieldType) match {
        case Some(v) => JsSuccess(v)
        case None => JsError(s"Invalid field type: $fieldType")
      }
    }

    override def writes(dataType: DataType.DataType): JsValue = {
      JsString(dataType.toString)
    }

  }

  implicit val appendFormat: Format[UnresolvedAppendStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "definition").format[String] and
      (JsPath \ "type").format[DataType.DataType] and
      (JsPath \ "as").format[String]
    ) (UnresolvedAppendStatement.apply, unlift(UnresolvedAppendStatement.unapply))


  implicit val lookupFormat: Format[UnresolvedLookupStatement] = (
    (JsPath \ "joinKey").format[Seq[String]] and
      (JsPath \ "dataset").format[String] and
      (JsPath \ "lookupKey").format[Seq[String]] and
      (JsPath \ "select").format[Seq[String]] and
      (JsPath \ "as").format[Seq[String]]
    ) (UnresolvedLookupStatement.apply, unlift(UnresolvedLookupStatement.unapply))

  implicit val groupFormat: Format[UnresolvedGroupStatement] = (
    (JsPath \ "by").format[Seq[UnresolvedByStatement]] and
      (JsPath \ "aggregate").format[Seq[UnresolvedAggregateStatement]] and
      (JsPath \ "lookup").formatNullable[Seq[UnresolvedLookupStatement]].inmap[Seq[UnresolvedLookupStatement]](
        o => o.getOrElse(Seq.empty[UnresolvedLookupStatement]),
        s => if (s.isEmpty) None else Some(s)
      )
    ) (UnresolvedGroupStatement.apply, unlift(UnresolvedGroupStatement.unapply))

  implicit val globalFormat: Format[UnresolvedGlobalAggregateStatement] = {
    (JsPath \ "globalAggregate").format[UnresolvedAggregateStatement].inmap(UnresolvedGlobalAggregateStatement.apply, unlift(UnresolvedGlobalAggregateStatement.unapply))
  }
  implicit val selectFormat: Format[UnresolvedSelectStatement] = (
    (JsPath \ "order").format[Seq[String]] and
      (JsPath \ "limit").format[Int] and
      (JsPath \ "offset").format[Int] and
      (JsPath \ "field").formatNullable[Seq[String]].inmap[Seq[String]](
        o => o.getOrElse(Seq.empty[String]),
        s => if (s.isEmpty) None else Some(s)
      )
    ) (UnresolvedSelectStatement.apply, unlift(UnresolvedSelectStatement.unapply))


  implicit val unnestFormat: Format[UnresolvedUnnestStatement] = new Format[UnresolvedUnnestStatement] {
    override def reads(json: JsValue): JsResult[UnresolvedUnnestStatement] = {
      JsSuccess(json.as[JsObject].value.map {
        case (key, jsValue: JsValue) =>
          UnresolvedUnnestStatement(key, jsValue.as[String])
      }.head)
    }

    override def writes(unnestStatement: UnresolvedUnnestStatement): JsValue = {
      JsObject(Seq(unnestStatement.field -> JsString(unnestStatement.as)))
    }
  }

  implicit val filterFormat: Format[UnresolvedFilterStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "apply").formatNullable[TransformFunc] and
      (JsPath \ "relation").format[Relation] and
      (JsPath \ "values").format[Seq[Any]]
    ) (UnresolvedFilterStatement.apply, unlift(UnresolvedFilterStatement.unapply))

  implicit val exeOptionReads: Reads[QueryExeOption] = (
    (__ \ QueryExeOption.TagSliceMillis).readNullable[Int].map(_.getOrElse(-1)) and
      (__ \ QueryExeOption.TagContinueSeconds).readNullable[Int].map(_.getOrElse(-1)) and
      // this "limit" value will be discarded, real limit value is parsed from request
      (__ \ QueryExeOption.TagLimit).readNullable[Int]
    ) (QueryExeOption.apply _)


  // TODO find better name for 'global'
  implicit val queryFormat: Format[UnresolvedQuery] = (
    (JsPath \ "dataset").format[String] and
      (JsPath \ "append").formatNullable[Seq[UnresolvedAppendStatement]].inmap[Seq[UnresolvedAppendStatement]](
        o => o.getOrElse(Seq.empty[UnresolvedAppendStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "lookup").formatNullable[Seq[UnresolvedLookupStatement]].inmap[Seq[UnresolvedLookupStatement]](
        o => o.getOrElse(Seq.empty[UnresolvedLookupStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "filter").formatNullable[Seq[UnresolvedFilterStatement]].inmap[Seq[UnresolvedFilterStatement]](
        o => o.getOrElse(Seq.empty[UnresolvedFilterStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "unnest").formatNullable[Seq[UnresolvedUnnestStatement]].inmap[Seq[UnresolvedUnnestStatement]](
        o => o.getOrElse(Seq.empty[UnresolvedUnnestStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "group").formatNullable[UnresolvedGroupStatement] and
      (JsPath \ "select").formatNullable[UnresolvedSelectStatement] and
      (JsPath \ "global").formatNullable[UnresolvedGlobalAggregateStatement] and
      (JsPath \ "estimable").formatNullable[Boolean].inmap[Boolean](
        o => o.getOrElse(false),
        s => if (s) Some(s) else None
      )
    ) (UnresolvedQuery.apply, unlift(UnresolvedQuery.unapply))

}
