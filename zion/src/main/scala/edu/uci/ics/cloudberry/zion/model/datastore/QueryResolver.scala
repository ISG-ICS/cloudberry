package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.Relation._
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection.mutable
import scala.reflect.runtime.universe.{TypeTag, typeOf}


class QueryValidator {
  private def requireOrThrow(condition: Boolean, msg: => String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }


  private def isAnyNumber[T: TypeTag](t: T): Boolean = {
    t.isInstanceOf[Number] || implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
  }

  def validateQuery(query: Query): Unit = {
    requireOrThrow(query.select.isDefined || query.groups.isDefined || query.globalAggr.isDefined,
      "either group or select or global aggregate statement is required")
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
      case DataType.Boolean => ???
      case DataType.String => ???
      case DataType.Text =>
        validateTextRelation(filter.relation, filter.values)
      case DataType.Bag => ???
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

  def validateGroupby(by: ByStatement): Unit = {
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
    if (relation != inRange) {
      throw new QueryParsingException(s"point type doesn't support relation $relation")
    }


  }

  def validateTextRelation(relation: Relation, values: Seq[Any]): Unit = {
    requireOrThrow(values.forall(_.isInstanceOf[String]), s"the ${relation} on text type requires string parameters.")
  }


}


/**
  * perform semantic analysis of cloudberry queries
  */
class QueryResolver {

  val validator = new QueryValidator

  def analyze(query: IQuery, schemaMap: Map[String, Schema]): Unit = {
    query match {
      case q: Query =>
        validator.validateQuery(q)
        resolveQuery(q, schemaMap)
      case q: CreateView => resolveCreate(q, schemaMap)
      case q: AppendView => resolveAppend(q, schemaMap)
      case q: UpsertRecord => resolveUpsert(q, schemaMap)
      case q: DropView => ???
      case _ => ???
    }
  }

  private def resolveQuery(query: Query, schemaMap: Map[String, Schema]): Unit = {
    val schema = schemaMap(query.dataset)
    val fieldMap = schema.fieldMap
    val fieldMapAfterLookup = resolveLookups(query.lookup, fieldMap, schemaMap)
    val fieldMapAfterUnnest = resolveUnnests(query.unnest, fieldMapAfterLookup)

    resolveFilters(query.filter, fieldMapAfterUnnest)

    val fieldMapAfterGroup = resolveGroup(query.groups, fieldMapAfterUnnest)
    val fieldMapAfterSelect = resolveSelect(query.select, fieldMapAfterGroup)

    val fieldMapAfterGlobalAggr = resolveGlobalAggregate(query.globalAggr, fieldMapAfterSelect)
  }

  private def resolveLookups(lookups: Seq[LookupStatement],
                             fieldMap: Map[String, Field],
                             schemaMap: Map[String, Schema]): Map[String, Field] = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    lookups.foreach { lookup =>
      validator.validateLookup(lookup)
      lookup.sourceKeyFields = resolveFields(lookup.sourceKeys, fieldMap)
      lookup.lookupKeyFields = resolveFields(lookup.lookupKeys, schemaMap(lookup.dataset).fieldMap)
      lookup.selectValueFields = resolveFields(lookup.selectValues, schemaMap(lookup.dataset).fieldMap)
      lookup.asFields = lookup.as.zip(lookup.selectValueFields).map {
        case (as, field) =>
          val asField = field.asField(as)
          producedFields += as -> asField
          asField
      }
    }
    (producedFields ++= fieldMap).result().toMap
  }

  private def resolveUnnests(unnests: Seq[UnnestStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    unnests.foreach { unnest =>
      unnest.field = resolveField(unnest.fieldName, fieldMap)
      unnest.asField = unnest.field.asField(unnest.as)
      producedFields += unnest.as -> unnest.asField
      validator.validateUnnest(unnest)
    }


    (producedFields ++= fieldMap).result().toMap
  }

  private def resolveFilters(filters: Seq[FilterStatement], fieldMap: Map[String, Field]): Unit = {
    filters.foreach { filter =>
      filter.field = resolveField(filter.fieldName, fieldMap)
      validator.validateFilter(filter)
    }
  }

  private def resolveGroup(group: Option[GroupStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    if (!group.isDefined) {
      return fieldMap
    }
    val producedFields = mutable.Map.newBuilder[String, Field]
    val groupStatement = group.get

    groupStatement.bys.foreach { by =>
      by.field = resolveField(by.fieldName, fieldMap)
      //TODO: if group function exists, then there must exist an as?
      val groupedField = by.funcOpt match {
        case Some(func) => func(by.field)
        case None => by.field
      }
      by.asField = by.as match {
        case Some(as) =>
          val asField = groupedField.asField(as)
          producedFields += as -> asField
          Some(asField)
        case None =>
          producedFields += groupedField.name -> groupedField
          None
      }
      validator.validateGroupby(by)
    }

    groupStatement.aggregates.foreach { aggregate =>
      aggregate.field = resolveField(aggregate.fieldName, fieldMap)
      aggregate.asField = aggregate.func(aggregate.field).asField(aggregate.as)
      producedFields += aggregate.as -> aggregate.asField
    }

    producedFields.result().toMap
  }

  private def resolveSelect(select: Option[SelectStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    if (!select.isDefined) {
      return fieldMap
    }
    val selectStatement = select.get

    selectStatement.orderOnFields = resolveFields(selectStatement.orderOn.map(selectStatement.truncate(_)), fieldMap)
    selectStatement.fields = resolveFields(selectStatement.fieldNames, fieldMap)

    if (selectStatement.fields.isEmpty) {
      return fieldMap
    } else {
      return selectStatement.fields.map {
        field => field.name -> field
      }.toMap
    }

  }

  private def resolveGlobalAggregate(globalAggregate: Option[GlobalAggregateStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    if (!globalAggregate.isDefined) {
      return fieldMap
    }
    val aggregate = globalAggregate.get.aggregate
    aggregate.field = resolveField(aggregate.fieldName, fieldMap)
    aggregate.asField = aggregate.func(aggregate.field).asField(aggregate.as)

    return Map(aggregate.as -> aggregate.asField)
  }


  private def resolveCreate(query: CreateView, schemaMap: Map[String, Schema]): Unit = {
    resolveQuery(query.query, schemaMap)
  }

  private def resolveAppend(query: AppendView, schemaMap: Map[String, Schema]): Unit = {
    resolveQuery(query.query, schemaMap)
  }

  private def resolveUpsert(query: UpsertRecord, schemaMap: Map[String, Schema]): Unit = {
    //do nothing
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
