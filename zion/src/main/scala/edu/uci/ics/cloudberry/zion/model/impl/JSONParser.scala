package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{FieldNotFound, IJSONParser, JsonRequestException, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, _}

import scala.collection.mutable
import scala.reflect.runtime.universe.{TypeTag, typeOf}

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


  implicit def toUnresolved(query: Query): UnresolvedQuery =
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

  implicit def toUnresolved(lookup: LookupStatement): UnresolvedLookupStatement =
    UnresolvedLookupStatement(
      lookup.sourceKeys.map(_.name),
      lookup.dataset,
      lookup.lookupKeys.map(_.name),
      lookup.selectValues.map(_.name),
      lookup.as.map(_.name)
    )

  implicit def toUnresolved(aggr: AggregateStatement): UnresolvedAggregateStatement =
    UnresolvedAggregateStatement(
      aggr.field.name,
      aggr.func,
      aggr.as.name
    )

  implicit def toUnresolved(filter: FilterStatement): UnresolvedFilterStatement =
    UnresolvedFilterStatement(
      filter.field.name,
      filter.funcOpt,
      filter.relation,
      filter.values
    )

  implicit def toUnresolved(unnest: UnnestStatement): UnresolvedUnnestStatement =
    UnresolvedUnnestStatement(
      unnest.field.name,
      unnest.as.name
    )

  implicit def toUnresolved(by: ByStatement): UnresolvedByStatement =
    UnresolvedByStatement(
      by.field.name,
      by.funcOpt,
      by.as.map(_.name)
    )

  implicit def toUnresolved(group: GroupStatement): UnresolvedGroupStatement =
    UnresolvedGroupStatement(
      group.bys.map(toUnresolved(_)),
      group.aggregates.map(toUnresolved(_))
    )


  implicit def toUnresolved(globalAggr: GlobalAggregateStatement): UnresolvedGlobalAggregateStatement =
    UnresolvedGlobalAggregateStatement(
      globalAggr.aggregate
    )

  implicit def toUnresolved(select: SelectStatement): UnresolvedSelectStatement =
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

/**
  * perform semantic analysis of cloudberry queries
  */
class QueryResolver {


  def resolve(query: IQuery, schemaMap: Map[String, Schema]): IQuery = {
    query match {
      case q: UnresolvedQuery =>
        resolveQuery(q, schemaMap)
      case q: CreateView => ???
      case q: AppendView => ???
      case q: UpsertRecord => ???
      case q: DropView => ???
      case _ => ???
    }
  }

  private def resolveQuery(query: UnresolvedQuery, schemaMap: Map[String, Schema]): Query = {
    val schema = schemaMap(query.dataset)
    val fieldMap = schema.fieldMap
    val (lookup, fieldMapAfterLookup) = resolveLookups(query.lookup, fieldMap, schemaMap)
    val (unnest, fieldMapAfterUnnest) = resolveUnnests(query.unnest, fieldMapAfterLookup)

    val (filter, fieldMapAfterFilter) = resolveFilters(query.filter, fieldMapAfterUnnest)

    val (groups, fieldMapAfterGroup) = resolveGroup(query.groups, fieldMapAfterFilter)
    val (select, fieldMapAfterSelect) = resolveSelect(query.select, fieldMapAfterGroup)

    val (globalAggr, fieldMapAfterGlobalAggr) = resolveGlobalAggregate(query.globalAggr, fieldMapAfterSelect)

    Query(query.dataset, lookup, filter, unnest, groups, select, globalAggr, query.estimable)
  }

  private def resolveLookups(lookups: Seq[UnresolvedLookupStatement],
                             fieldMap: Map[String, Field],
                             schemaMap: Map[String, Schema]): (Seq[LookupStatement], Map[String, Field]) = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    val resolved = lookups.map { lookup =>

      val sourceKeys = resolveFields(lookup.sourceKeys, fieldMap)
      val lookupKeys = resolveFields(lookup.lookupKeys, schemaMap(lookup.dataset).fieldMap)
      val selectValues = resolveFields(lookup.selectValues, schemaMap(lookup.dataset).fieldMap)
      val as = lookup.as.zip(selectValues).map {
        case (as, field) =>
          val asField = Field.as(field, as)
          producedFields += as -> asField
          asField
      }
      LookupStatement(sourceKeys, lookup.dataset, lookupKeys, selectValues, as)
    }

    (resolved, (producedFields ++= fieldMap).result().toMap)
  }

  private def resolveUnnests(unnests: Seq[UnresolvedUnnestStatement], fieldMap: Map[String, Field]): (Seq[UnnestStatement], Map[String, Field]) = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    val resolved = unnests.map { unnest =>
      val field = resolveField(unnest.field, fieldMap)
      val asField = Field.as(field, unnest.as)
      producedFields += unnest.as -> asField
      UnnestStatement(field, asField)
    }

    (resolved, (producedFields ++= fieldMap).result().toMap)
  }

  private def resolveFilters(filters: Seq[UnresolvedFilterStatement], fieldMap: Map[String, Field]): (Seq[FilterStatement], Map[String, Field]) = {
    val resolved = filters.map { filter =>
      val field = resolveField(filter.field, fieldMap)
      FilterStatement(field, filter.funcOpt, filter.relation, filter.values)
    }
    (resolved, fieldMap)
  }

  private def resolveGroup(group: Option[UnresolvedGroupStatement], fieldMap: Map[String, Field]): (Option[GroupStatement], Map[String, Field]) = {
    group match {
      case Some(groupStatement) =>
        val producedFields = mutable.Map.newBuilder[String, Field]
        val resolvedBys = groupStatement.bys.map { by =>
          val field = resolveField(by.field, fieldMap)
          val groupedField = by.funcOpt match {
            case Some(func) => func(field)
            case None => field
          }
          val as = by.as match {
            case Some(as) =>
              val asField = Field.as(groupedField, as)
              producedFields += as -> asField
              Some(asField)
            case None =>
              producedFields += groupedField.name -> groupedField
              None
          }
          ByStatement(field, by.funcOpt, as)
        }
        val resolvedAggrs = groupStatement.aggregates.map { aggregate =>
          val field = resolveField(aggregate.field, fieldMap)
          val as = Field.as(aggregate.func(field), aggregate.as)
          producedFields += aggregate.as -> as
          AggregateStatement(field, aggregate.func, as)
        }

        val resolved = GroupStatement(resolvedBys, resolvedAggrs)
        (Some(resolved), producedFields.result().toMap)
      case None =>
        (None, fieldMap)
    }
  }

  private def resolveSelect(select: Option[UnresolvedSelectStatement], fieldMap: Map[String, Field]): (Option[SelectStatement], Map[String, Field]) = {
    select match {
      case Some(selectStatement) =>
        val orderOn = resolveFields(selectStatement.orderOn.map { field =>
          if (field.startsWith("-")) {
            field.substring(1)
          } else {
            field
          }
        }, fieldMap)
        val order = selectStatement.orderOn.map { field =>
          if (field.startsWith("-")) {
            SortOrder.DSC
          } else {
            SortOrder.ASC
          }
        }
        val fields = resolveFields(selectStatement.fields, fieldMap)
        val resolved = SelectStatement(orderOn, order, selectStatement.limit, selectStatement.offset, fields)
        val newFieldMap =
          if (selectStatement.fields.isEmpty) {
            fieldMap
          } else {
            fields.map {
              field => field.name -> field
            }.toMap
          }
        (Some(resolved), newFieldMap)
      case None => (None, fieldMap)
    }

  }

  private def resolveGlobalAggregate(globalAggregate: Option[UnresolvedGlobalAggregateStatement], fieldMap: Map[String, Field]): (Option[GlobalAggregateStatement], Map[String, Field]) = {
    globalAggregate match {
      case Some(globalAggregateStatement) =>
        val aggregate = globalAggregateStatement.aggregate
        val field = resolveField(aggregate.field, fieldMap)
        val asField = Field.as(aggregate.func(field), aggregate.as)
        val resolved = GlobalAggregateStatement(AggregateStatement(field, aggregate.func, asField))
        (Some(resolved), Map(aggregate.as -> asField))
      case None => (None, fieldMap)
    }
  }

  private def resolveFields(names: Seq[String], fieldMap: Map[String, Field]): Seq[Field] = {
    names.map {
      name => resolveField(name, fieldMap)
    }
  }

  private def resolveField(name: String, fieldMap: Map[String, Field]): Field = {
    val field = fieldMap.get(name)
    field match {
      case Some(f) => f
      case _ => throw FieldNotFound(name)
    }
  }
}

class QueryValidator {
  def validate(query: IQuery): Unit = {
    query match {
      case q: Query =>
        validateQuery(q)
      case q: CreateView => validateCreate(q)
      case q: AppendView => validateAppend(q)
      case q: UpsertRecord => validateUpsert(q)
      case q: DropView => ???
      case _ => ???
    }
  }

  private def validateCreate(q: CreateView): Unit = {
    validateQuery(q.query)
  }

  private def validateAppend(q: AppendView): Unit = {
    validateQuery(q.query)
  }

  private def validateUpsert(q: UpsertRecord): Unit = {

  }

  private def requireOrThrow(condition: Boolean, msg: => String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }


  private def isAnyNumber[T: TypeTag](t: T): Boolean = {
    t.isInstanceOf[Number] || implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
  }

  def validateQuery(query: Query): Unit = {
    //   requireOrThrow(query.select.isDefined || query.groups.isDefined || query.globalAggr.isDefined,
    //     "either group or select or global aggregate statement is required")
    query.filter.foreach(validateFilter(_))
    query.lookup.foreach(validateLookup(_))
    query.unnest.foreach(validateUnnest(_))
    query.groups.foreach(validateGroup(_))
  }

  def validateFilter(filter: FilterStatement): Unit = {
    requireOrThrow(Schema.Type2Relations(filter.field.dataType).contains(filter.relation),
      s"field ${filter.field.name} of type ${filter.field.dataType} can not apply to relation: ${filter.relation}."
    )

    filter.field.dataType match {
      case DataType.Number =>
        validateNumberRelation(filter.relation, filter.values)
      case DataType.Time =>
        validateTimeRelation(filter.relation, filter.values)
      case DataType.Point =>
        validatePointRelation(filter.relation, filter.values)
      case DataType.Boolean =>
      case DataType.String =>
      case DataType.Text =>
        validateTextRelation(filter.relation, filter.values)
      case DataType.Bag =>
      case DataType.Hierarchy =>
        throw new QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${filter.field.dataType}")
    }
  }

  def validateUnnest(unnest: UnnestStatement): Unit = {
    require(unnest.field.isInstanceOf[BagField], "unnest can only apply on Bag type")
  }

  def validateLookup(lookup: LookupStatement): Unit = {
    requireOrThrow(lookup.sourceKeys.length == lookup.lookupKeys.length, "LookupStatement: lookup key number is different from size of the source key ")
    requireOrThrow(lookup.selectValues.length == lookup.as.length, "LookupStatement: select value names doesn't match with renamed names")
  }

  def validateGroup(group: GroupStatement): Unit = {
    requireOrThrow(group.bys.nonEmpty, "By statement is required")
    requireOrThrow(group.aggregates.nonEmpty, "Aggregation statement is required")

    group.bys.foreach(validateBy(_))
  }

  def validateBy(by: ByStatement): Unit = {
    by.funcOpt match {
      case Some(func) =>
        func match {
          case level: Level =>
            val hierarchyField = by.field.asInstanceOf[HierarchyField]
            requireOrThrow(hierarchyField.levels.find(_._1 == level.levelTag).isDefined, s"could not find the level tag ${level.levelTag} in hierarchy field ${hierarchyField.name}")
          case _ =>
        }
      case None =>
    }
  }

  def validateNumberRelation(relation: Relation, values: Seq[Any]): Unit = {
    requireOrThrow(values.forall(isAnyNumber), s"values contain non compatible data type for relation: $relation.")

    relation match {
      case Relation.inRange =>
        if (values.size != 2) throw new QueryParsingException(s"relation: $relation require two parameters")
      case Relation.in =>
      case _ =>
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
    }
  }

  def validateTimeRelation(relation: Relation, values: Seq[Any]): Unit = {
    //required string format : http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
    try {
      if (!values.forall(_.isInstanceOf[String])) {
        throw new IllegalArgumentException
      }
      // This parseDateTime will throw an exception if the format is invalid
      values.foreach(t => TimeField.TimeFormat.parseDateTime(t.asInstanceOf[String]))
    } catch {
      case _: IllegalArgumentException => throw new QueryParsingException("invalid time format")
    }

    relation match {
      case Relation.inRange => {
        if (values.size != 2) {
          throw new QueryParsingException(s"relation: $relation require two parameters.")
        }
      }
      case _ => {
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter.")
      }
    }
  }

  def validatePointRelation(relation: Relation, values: Seq[Any]): Unit = {
    if (!values.forall(_.isInstanceOf[Seq[_]]) || values.size != 2
      || !values.map(_.asInstanceOf[Seq[_]]).forall(ary => ary.size == 2 && ary.forall(isAnyNumber))) {
      throw new QueryParsingException(s"the ${relation} on point type requires a pair of value pairs.")
    }
    if (relation != Relation.inRange) {
      throw new QueryParsingException(s"point type doesn't support relation $relation")
    }
  }

  def validateTextRelation(relation: Relation, values: Seq[Any]): Unit = {
    requireOrThrow(values.forall(_.isInstanceOf[String]), s"the ${relation} on text type requires string parameters.")
  }


}
