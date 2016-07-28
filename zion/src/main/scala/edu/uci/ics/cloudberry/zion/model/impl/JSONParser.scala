package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IJSONParser, JsonRequestException}
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class JSONParser extends IJSONParser {

  import JSONParser._

  override def parse(json: JsValue): Query = {
    json.validate[Query] match {
      case js: JsSuccess[Query] => js.get
      case e: JsError => throw JsonRequestException(JsError.toJson(e).toString())
    }
  }
}

object JSONParser {
  //Warn: the order of implicit values matters. The dependence should be initialized earlier

  implicit val seqAnyValue: Reads[Seq[Any]] = new Reads[Seq[Any]] {
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
  }

  implicit val transformFuncReads: Reads[TransformFunc] = new Reads[TransformFunc] {
    override def reads(json: JsValue): JsResult[TransformFunc] = ???
  }

  implicit val relationReads: Reads[Relation] = new Reads[Relation] {
    override def reads(json: JsValue): JsResult[Relation] = {
      try {
        JsSuccess(Relation.withName(json.as[String]))
      } catch {
        case e: NoSuchElementException => JsError(s"unknown relation: $json")
      }
    }
  }


  implicit val groupFuncReads: Reads[GroupFunc] = new Reads[GroupFunc] {
    override def reads(json: JsValue): JsResult[GroupFunc] = (json \ "name").as[String] match {
      case GroupFunc.Bin => ???
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
  }

  implicit val aggFuncReads: Reads[AggregateFunc] = new Reads[AggregateFunc] {
    override def reads(json: JsValue): JsResult[AggregateFunc] = {
      (json \ "name").as[String] match {
        case AggregateFunc.Count => JsSuccess(Count)
        case AggregateFunc.TopK => ???
        case AggregateFunc.Sum => ???
        case AggregateFunc.Max => ???
        case AggregateFunc.Min => ???
        case AggregateFunc.Avg => ???
        case AggregateFunc.DistinctCount => ???
        case unknown: String => JsError(s"unknown aggregation function: $unknown")
      }
    }
  }

  implicit val aggReads: Reads[AggregateStatement] = {
    (JsPath \ "field").read[String] and
      (JsPath \ "apply").read[AggregateFunc] and
      (JsPath \ "as").read[String]
  }.apply(AggregateStatement.apply _)

  implicit val byReads: Reads[ByStatement] = {
    (JsPath \ "field").read[String] and
      (JsPath \ "apply").readNullable[GroupFunc] and
      (JsPath \ "as").readNullable[String]
  }.apply(ByStatement.apply _)

  implicit val groupReads: Reads[GroupStatement] = {
    (JsPath \ "by").read[Seq[ByStatement]] and
      (JsPath \ "aggregate").read[Seq[AggregateStatement]]
  }.apply(GroupStatement.apply _)

  implicit val selectReads: Reads[SelectStatement] = {
    (JsPath \ "order").read[Seq[String]] and
      (JsPath \ "limit").read[Int] and
      (JsPath \ "offset").read[Int] and
      (JsPath \ "field").readNullable[Seq[String]].map(_.getOrElse(Seq.empty))
  }.apply(SelectStatement.apply _)

  implicit val lookupReads: Reads[LookupStatement] = {
    (JsPath \ "sourceKey").read[Seq[String]] and
      (JsPath \ "dataset").read[String] and
      (JsPath \ "lookupKey").read[Seq[String]] and
      (JsPath \ "select").read[Seq[String]] and
      (JsPath \ "as").read[Seq[String]]
  }.apply(LookupStatement.apply _)

  implicit val unnestReads: Reads[Seq[UnnestStatement]] = new Reads[Seq[UnnestStatement]] {
    override def reads(json: JsValue): JsResult[Seq[UnnestStatement]] = {
      JsSuccess(json.as[JsObject].value.map {
        case (key, jsValue: JsValue) =>
          UnnestStatement(key, jsValue.as[String])
      }.toSeq)
    }
  }

  implicit val filterReads: Reads[FilterStatement] = {
    (JsPath \ "field").read[String] and
      (JsPath \ "apply").readNullable[TransformFunc] and
      (JsPath \ "relation").read[Relation] and
      (JsPath \ "values").read[Seq[Any]]
  }.apply(FilterStatement.apply _)

  implicit val queryReads: Reads[Query] = {
    (JsPath \ "dataset").read[String] and
      (JsPath \ "lookup").readNullable[Seq[LookupStatement]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "filter").readNullable[Seq[FilterStatement]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "unnest").readNullable[Seq[UnnestStatement]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "group").readNullable[GroupStatement] and
      (JsPath \ "select").readNullable[SelectStatement]
  }.apply(Query.apply _)

}
