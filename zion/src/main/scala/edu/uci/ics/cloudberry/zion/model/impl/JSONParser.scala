package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IJSONParser, JsonRequestException}
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, _}

class JSONParser extends IJSONParser {

  import JSONParser._

  override def parse(json: JsValue): (Seq[Query], QueryExeOption) = {
    val option = (json \ "option").toOption.map(_.as[QueryExeOption]).getOrElse(QueryExeOption.NoSliceNoContinue)
    val query = (json \ "batch").toOption match {
      case Some(groupRequest) => groupRequest.validate[Seq[Query]] match {
        case js: JsSuccess[Seq[Query]] => js.get
        case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
      }
      case None => json.validate[Query] match {
        case js: JsSuccess[Query] => Seq(js.get)
        case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
      }
    }
    (query, option)
  }
}

object JSONParser {
  //Warn: the order of implicit values matters. The dependence should be initialized earlier

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
        case fs: FilterStatement => filterFormat.writes(fs)
        case by: ByStatement => byFormat.writes(by)
        case ags: AggregateStatement => aggFormat.writes(ags)
        case unS: UnnestStatement => unnestFormat.writes(unS)
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
        case fGeoCellScale: GeoCellScale => JsObject(Seq("name" -> JsString(groupFunc.name)))
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

  implicit val aggFormat: Format[AggregateStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "apply").format[AggregateFunc] and
      (JsPath \ "as").format[String]
    ) (AggregateStatement.apply, unlift(AggregateStatement.unapply))

  implicit val byFormat: Format[ByStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "apply").formatNullable[GroupFunc] and
      (JsPath \ "as").formatNullable[String]
    ) (ByStatement.apply, unlift(ByStatement.unapply))

  implicit val groupFormat: Format[GroupStatement] = (
    (JsPath \ "by").format[Seq[ByStatement]] and
      (JsPath \ "aggregate").format[Seq[AggregateStatement]]
    ) (GroupStatement.apply, unlift(GroupStatement.unapply))

  implicit val globalFormat: Format[GlobalAggregateStatement] = {
    (JsPath \ "globalAggregate").format[AggregateStatement].inmap(GlobalAggregateStatement.apply, unlift(GlobalAggregateStatement.unapply))
  }
  implicit val selectFormat: Format[SelectStatement] = (
    (JsPath \ "order").format[Seq[String]] and
      (JsPath \ "limit").format[Int] and
      (JsPath \ "offset").format[Int] and
      (JsPath \ "field").formatNullable[Seq[String]].inmap[Seq[String]](
        o => o.getOrElse(Seq.empty[String]),
        s => if (s.isEmpty) None else Some(s)
      )
    ) (SelectStatement.apply, unlift(SelectStatement.unapply))

  implicit val lookupFormat: Format[LookupStatement] = (
    (JsPath \ "sourceKey").format[Seq[String]] and
      (JsPath \ "dataset").format[String] and
      (JsPath \ "lookupKey").format[Seq[String]] and
      (JsPath \ "select").format[Seq[String]] and
      (JsPath \ "as").format[Seq[String]]
    ) (LookupStatement.apply, unlift(LookupStatement.unapply))

  implicit val unnestFormat: Format[UnnestStatement] = new Format[UnnestStatement] {
    override def reads(json: JsValue): JsResult[UnnestStatement] = {
      JsSuccess(json.as[JsObject].value.map {
        case (key, jsValue: JsValue) =>
          UnnestStatement(key, jsValue.as[String])
      }.head)
    }

    override def writes(unnestStatement: UnnestStatement): JsValue = {
      JsObject(Seq(unnestStatement.fieldName -> JsString(unnestStatement.as)))
    }
  }

  implicit val filterFormat: Format[FilterStatement] = (
    (JsPath \ "field").format[String] and
      (JsPath \ "apply").formatNullable[TransformFunc] and
      (JsPath \ "relation").format[Relation] and
      (JsPath \ "values").format[Seq[Any]]
    ) (FilterStatement.apply, unlift(FilterStatement.unapply))

  implicit val exeOptionReads: Reads[QueryExeOption] = (
    (__ \ QueryExeOption.TagSliceMillis).readNullable[Int].map(_.getOrElse(-1)) and
      (__ \ QueryExeOption.TagContinueSeconds).readNullable[Int].map(_.getOrElse(-1))
    ) (QueryExeOption.apply _)


  // TODO find better name for 'global'
  implicit val queryFormat: Format[Query] = (
    (JsPath \ "dataset").format[String] and
      (JsPath \ "lookup").formatNullable[Seq[LookupStatement]].inmap[Seq[LookupStatement]](
        o => o.getOrElse(Seq.empty[LookupStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "filter").formatNullable[Seq[FilterStatement]].inmap[Seq[FilterStatement]](
        o => o.getOrElse(Seq.empty[FilterStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "unnest").formatNullable[Seq[UnnestStatement]].inmap[Seq[UnnestStatement]](
        o => o.getOrElse(Seq.empty[UnnestStatement]),
        s => if (s.isEmpty) None else Some(s)
      ) and
      (JsPath \ "group").formatNullable[GroupStatement] and
      (JsPath \ "select").formatNullable[SelectStatement] and
      (JsPath \ "global").formatNullable[GlobalAggregateStatement]
    ) (Query.apply, unlift(Query.unapply))

}
