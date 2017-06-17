package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.Logger

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object QueryValidator {
  /**
    * Check the semantic validity of a given query
    *
    * @param query
    */
  def validate(query: IQuery, schemaMap: Map[String, AbstractSchema]): Unit = {
    query match {
      case q: Query =>
        validateQuery(q, schemaMap)
      case q: CreateView => validateCreate(q, schemaMap)
      case q: AppendView => validateAppend(q, schemaMap)
      case q: UpsertRecord => validateUpsert(q, schemaMap)
      case q: DropView => ???
      case _ => ???
    }
  }

  private def validateCreate(q: CreateView, schemaMap: Map[String, AbstractSchema]): Unit = {
    validateQuery(q.query, schemaMap)
  }

  private def validateAppend(q: AppendView, schemaMap: Map[String, AbstractSchema]): Unit = {
    validateQuery(q.query, schemaMap)
  }

  private def validateUpsert(q: UpsertRecord, schemaMap: Map[String, AbstractSchema]): Unit = {
    //TODO validate upsert
  }

  private def requireOrThrow(condition: Boolean, msg: => String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }


  private def isAnyNumber[T: TypeTag](t: T): Boolean = {
    t.isInstanceOf[Number] || implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
  }

  def validateQuery(query: Query, schemaMap: Map[String, AbstractSchema]): Unit = {
    if (!schemaMap(query.dataset).isInstanceOf[Schema]) {
      throw new IllegalArgumentException("lookup dataset " + query.dataset + " does not support query " + query.toString)
    }
    query.filter.foreach(validateFilter(_))
    query.lookup.foreach(validateLookup(_))
    query.unnest.foreach(validateUnnest(_))
    query.groups.foreach(validateGroup(_))
  }

  def validateFilter(filter: FilterStatement): Unit = {
    requireOrThrow(AbstractSchema.Type2Relations(filter.field.dataType).contains(filter.relation),
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
        validateStringRelation(filter.relation, filter.values)
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
    group.lookups.foreach(validateLookup(_))
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

  def validateStringRelation(relation: Relation, values: Seq[Any]): Unit = {
    requireOrThrow(values.forall(_.isInstanceOf[String]), s"values contain non compatible data type for relation: $relation.")

    relation match {
      case Relation.matches | Relation.!= =>
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
      case Relation.contains => ???
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
