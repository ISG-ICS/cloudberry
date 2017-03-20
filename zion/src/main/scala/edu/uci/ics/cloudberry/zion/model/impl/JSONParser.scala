package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{FieldNotFound, IJSONParser, JsonRequestException, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, _}


/**
  * Parse a [[JsValue]] with a schema map, and returns a [[Query]]
  * First parse the [[JsValue]] into a [[UnresolvedQuery]] using json parsing APIs provided by Play framework.
  * Then, calls [[QueryResolver]] to resolve it into a [[Query]], which has all fields resolved typed.
  * Finally, calls [[QueryValidator]] to validate the correctness of this query.
  */
class JSONParser extends IJSONParser {

  import JSONParser._

  override def getDatasets(json: JsValue): Set[String] = {
    val datasets = (json \\ "dataset").filter(_.isInstanceOf[JsString]).map(_.asInstanceOf[JsString].value)
    if (datasets.isEmpty) {
      datasets.toSet
    } else {
      //TODO currently do not handle lookup queries
      Set(datasets.head)
    }
  }

  override def parse(json: JsValue, schemaMap: Map[String, Schema]): (Seq[Query], QueryExeOption) = {
    val option = (json \ "option").toOption.map(_.as[QueryExeOption]).getOrElse(QueryExeOption.NoSliceNoContinue)
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

  val resolver = new QueryResolver

  val validator = new QueryValidator

  def resolve(query: UnresolvedQuery, schemaMap: Map[String, Schema]): Query = {
    val resolved = resolver.resolve(query, schemaMap).asInstanceOf[Query]
    validator.validate(resolved)
    resolved
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


  implicit val groupFormat: Format[UnresolvedGroupStatement] = (
    (JsPath \ "by").format[Seq[UnresolvedByStatement]] and
      (JsPath \ "aggregate").format[Seq[UnresolvedAggregateStatement]]
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

  implicit val lookupFormat: Format[UnresolvedLookupStatement] = (
    (JsPath \ "joinKey").format[Seq[String]] and
      (JsPath \ "dataset").format[String] and
      (JsPath \ "lookupKey").format[Seq[String]] and
      (JsPath \ "select").format[Seq[String]] and
      (JsPath \ "as").format[Seq[String]]
    ) (UnresolvedLookupStatement.apply, unlift(UnresolvedLookupStatement.unapply))

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
      (__ \ QueryExeOption.TagContinueSeconds).readNullable[Int].map(_.getOrElse(-1))
    ) (QueryExeOption.apply _)


  // TODO find better name for 'global'
  implicit val queryFormat: Format[UnresolvedQuery] = (
    (JsPath \ "dataset").format[String] and
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


  def toUnresolved(query: Query): UnresolvedQuery =
    UnresolvedQuery(
      query.dataset,
      query.lookup.map(toUnresolved(_)),
      query.filter.map(toUnresolved(_)),
      query.unnest.map(toUnresolved(_)),
      query.groups.map(toUnresolved(_)),
      query.select.map(toUnresolved(_)),
      query.globalAggr.map(toUnresolved(_)),
      query.isEstimable
    )

  def toUnresolved(lookup: LookupStatement): UnresolvedLookupStatement =
    UnresolvedLookupStatement(
      lookup.sourceKeys.map(_.name),
      lookup.dataset,
      lookup.lookupKeys.map(_.name),
      lookup.selectValues.map(_.name),
      lookup.as.map(_.name)
    )

  def toUnresolved(aggr: AggregateStatement): UnresolvedAggregateStatement =
    UnresolvedAggregateStatement(
      aggr.field.name,
      aggr.func,
      aggr.as.name
    )

  def toUnresolved(filter: FilterStatement): UnresolvedFilterStatement =
    UnresolvedFilterStatement(
      filter.field.name,
      filter.funcOpt,
      filter.relation,
      filter.values
    )

  def toUnresolved(unnest: UnnestStatement): UnresolvedUnnestStatement =
    UnresolvedUnnestStatement(
      unnest.field.name,
      unnest.as.name
    )

  def toUnresolved(by: ByStatement): UnresolvedByStatement =
    UnresolvedByStatement(
      by.field.name,
      by.funcOpt,
      by.as.map(_.name)
    )

  def toUnresolved(group: GroupStatement): UnresolvedGroupStatement =
    UnresolvedGroupStatement(
      group.bys.map(toUnresolved(_)),
      group.aggregates.map(toUnresolved(_))
    )


  def toUnresolved(globalAggr: GlobalAggregateStatement): UnresolvedGlobalAggregateStatement =
    UnresolvedGlobalAggregateStatement(
      toUnresolved(globalAggr.aggregate)
    )

  def toUnresolved(select: SelectStatement): UnresolvedSelectStatement =
    UnresolvedSelectStatement(
      select.orderOn.zip(select.order).map {
        case (field, order) =>
          order match {
            case SortOrder.DSC => "-" + field.name
            case SortOrder.ASC => field.name
          }
      },
      select.limit,
      select.offset,
      select.fields.map(_.name)

    )

}


case class UnresolvedQuery(dataset: String,
                           lookup: Seq[UnresolvedLookupStatement] = Seq.empty,
                           filter: Seq[UnresolvedFilterStatement] = Seq.empty,
                           unnest: Seq[UnresolvedUnnestStatement] = Seq.empty,
                           groups: Option[UnresolvedGroupStatement] = None,
                           select: Option[UnresolvedSelectStatement] = None,
                           globalAggr: Option[UnresolvedGlobalAggregateStatement] = None,
                           estimable: Boolean = false
                          ) extends IReadQuery

case class UnresolvedLookupStatement(sourceKeys: Seq[String],
                                     dataset: String,
                                     lookupKeys: Seq[String],
                                     selectValues: Seq[String],
                                     as: Seq[String]
                                    ) extends Statement

case class UnresolvedFilterStatement(field: String,
                                     funcOpt: Option[TransformFunc],
                                     relation: Relation,
                                     values: Seq[Any]
                                    ) extends Statement

case class UnresolvedUnnestStatement(field: String, as: String)

case class UnresolvedByStatement(field: String,
                                 funcOpt: Option[GroupFunc],
                                 as: Option[String]
                                ) extends Statement

case class UnresolvedAggregateStatement(field: String,
                                        func: AggregateFunc,
                                        as: String
                                       ) extends Statement

case class UnresolvedGroupStatement(bys: Seq[UnresolvedByStatement],
                                    aggregates: Seq[UnresolvedAggregateStatement]
                                   ) extends Statement

case class UnresolvedGlobalAggregateStatement(aggregate: UnresolvedAggregateStatement
                                             ) extends Statement

case class UnresolvedSelectStatement(orderOn: Seq[String],
                                     limit: Int,
                                     offset: Int,
                                     fields: Seq[String]
                                    ) extends Statement
