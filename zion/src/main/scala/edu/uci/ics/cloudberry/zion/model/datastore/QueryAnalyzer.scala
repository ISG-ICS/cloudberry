package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.Relation._
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection.mutable
import scala.reflect.runtime.universe.{TypeTag, typeOf}


/**
  * perform semantic analysis of cloudberry queries
  */
class QueryAnalyzer {

  private def requireOrThrow(condition: Boolean, msg: String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }

  def analyze(query: IQuery, schema: Schema): Unit = {
    query match {
      case q: Query =>
        resolveQuery(q, schema)
      case q: CreateView => resolveCreate(q, schema)
      case q: AppendView => resolveAppend(q, schema)
      case q: UpsertRecord => resolveUpsert(q, schema)
      case q: DropView => ???
      case _ => ???
    }
  }

  private def resolveQuery(query: Query, schema: Schema): Unit = {
    requireOrThrow(query.select.isDefined || query.group.isDefined || query.globalAggr.isDefined, "either group or select or global aggregate statement is required")

    val fieldMap: Map[String, Field] = schema.fieldMap
    val fieldMapAfterLookup = resolveLookups(query.lookups, fieldMap)
    val fieldMapAfterUnnest = resolveUnnests(query.unnests, fieldMapAfterLookup)

    resolveFilters(query.filters, fieldMapAfterUnnest)

    val fieldMapAfterGroup = resolveGroup(query.group, fieldMapAfterUnnest)
    val fieldMapAfterSelect = resolveSelect(query.select, fieldMapAfterGroup)

    val fieldMapAfterAggregate = resolveAggregate(query.globalAggr, fieldMapAfterSelect)
  }

  private def resolveLookups(lookups: Seq[LookupStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    lookups.foreach { lookup =>
      lookup.lookupKeyFields = resolveFields(lookup.lookupKeys, fieldMap)
      lookup.sourceKeyFields = resolveFields(lookup.sourceKeys, fieldMap)
      lookup.selectValueFields = resolveFields(lookup.selectValues, fieldMap)
      producedFields ++= lookup.as.zip(lookup.selectValueFields).map { p =>
        p._1 -> p._2.asField(p._1)
      }
    }
    (producedFields ++= fieldMap).result().toMap
  }

  private def resolveUnnests(unnests: Seq[UnnestStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    unnests.foreach { unnest =>
      unnest.field = resolveField(unnest.fieldName, fieldMap)
      unnest.asField = unnest.field.asField(unnest.as)
    }

    (producedFields ++= fieldMap).result().toMap
  }

  private def resolveFilters(filters: Seq[FilterStatement], fieldMap: Map[String, Field]): Unit = {
    filters.foreach { filter =>
      filter.field = resolveField(filter.fieldName, fieldMap)
      validateFilter(filter)
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
      by.asField = by.as match {
        case Some(as) =>
          val asField = by.field.asField(as)
          producedFields += as -> asField
          Some(asField)
        case None =>
          producedFields += by.fieldName -> by.field
          None
      }
    }

    groupStatement.aggregates.foreach { aggregate =>
      aggregate.field = resolveField(aggregate.fieldName, fieldMap)
      aggregate.asField = aggregate.field.asField(aggregate.as)
      producedFields += aggregate.as -> aggregate.asField
    }

    producedFields.result().toMap
  }

  private def resolveSelect(select: Option[SelectStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    if (!select.isDefined) {
      return fieldMap
    }
    val selectStatement = select.get

    selectStatement.orderOnFields = resolveFields(selectStatement.orderOn, fieldMap)
    selectStatement.fields = resolveFields(selectStatement.fieldNames, fieldMap)

    if (selectStatement.fields.isEmpty) {
      return fieldMap
    } else {
      return selectStatement.fields.map {
        field => field.name -> field
      }.toMap
    }

  }

  private def resolveGlobalStatement(globalAggregate: Option[GlobalAggregateStatement], fieldMap: Map[String, Field]): Map[String, Field] = {
    if (!globalAggregate.isDefined) {
      return fieldMap
    }
    val aggregate = globalAggregate.get.aggregate
    aggregate.field = resolveField(aggregate.fieldName, fieldMap)
    aggregate.asField = aggregate.field.asField(aggregate.as)

    return Map(aggregate.as -> aggregate.asField)
  }


  private def resolveCreate(query: CreateView, schema: Schema): Unit = {

  }

  private def resolveAppend(query: AppendView, schema: Schema): Unit = {

  }

  private def resolveUpsert(query: UpsertRecord, schema: Schema): Unit = {

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

  private def validateFilter(filter: FilterStatement): Unit = {
    if (!Schema.Type2Relations(filter.field.dataType).contains(filter.relation)) {
      throw new QueryParsingException(s"field ${filter.field.name} of type ${filter.field.dataType} can not apply to relation: ${filter.relation}")
    }
    filter.field.dataType match {
      case DataType.Number =>
        validateNumberValue(filter.relation, filter.values)
      case DataType.Time =>
        validateTimeValue(filter.relation, filter.values)
      case DataType.Point =>
        validatePointValue(filter.relation, filter.values)
      case DataType.Boolean => ???
      case DataType.String => ???
      case DataType.Text =>
        validateTextValue(filter.relation, filter.values)
      case DataType.Bag => ???
      case DataType.Hierarchy =>
        throw new QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${filter.field.dataType}")
    }
  }

  protected def validateNumberValue(relation: Relation, values: Seq[Any]): Unit = {
    if (!values.forall(isAnyNumber)) {
      throw new QueryParsingException(s"values contain non compatible data type for relation: $relation")
    }
  }

  protected def isAnyNumber[T: TypeTag](t: T): Boolean = {
    t.isInstanceOf[Number] || implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
  }

  protected def validateTimeValue(relation: Relation, values: Seq[Any]) = {
    //required string format : http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
    try {
      if (!values.forall(_.isInstanceOf[String])) {
        throw new IllegalArgumentException
      }
      // This parseDateTime will throw an exception if the format is invalid
      values.foreach(t => TimeField.TimeFormat.parseDateTime(t.asInstanceOf[String]))
    } catch {
      case ex: IllegalArgumentException => throw new QueryParsingException("invalid time format")
    }
  }

  protected def validatePointValue(relation: Relation, values: Seq[Any]) = {
    //TODO support circle and polygon
    if (!values.forall(_.isInstanceOf[Seq[_]]) || values.size != 2
      || !values.map(_.asInstanceOf[Seq[_]]).forall(ary => ary.size == 2 && ary.forall(isAnyNumber))) {
      throw new QueryParsingException(s"the ${
        relation
      } on point type requires a pair of value pairs")
    }
  }

  protected def validateTextValue(relation: Relation, values: Seq[Any]) = {
    if (!values.forall(_.isInstanceOf[String])) {
      throw new QueryParsingException(s"the ${
        relation
      } on text type requires string parameters")
    }
  }

}

object QueryAnalyzer {
  def apply() = new QueryAnalyzer
}