package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.FieldNotFound
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection.mutable

/**
  * Performs field resolution for [[UnresolvedQuery]]
  */
object QueryResolver {

  /**
    * Resolves an [[UnresolvedQuery]], and returns a [[Query]] based on the given schemaMap
    *
    * @param query
    * @param schemaMap
    * @return
    */
  def resolve(query: IQuery, schemaMap: Map[String, AbstractSchema]): IQuery = {
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

  private def resolveQuery(query: UnresolvedQuery, schemaMap: Map[String, AbstractSchema]): Query = {
    val schema = schemaMap(query.dataset)
    val fieldMap = schema.fieldMap
    val (append, fieldMapAfterAppend) = resolveAppends(query.append, fieldMap)
    val (lookup, fieldMapAfterLookup) = resolveLookups(query.lookup, fieldMapAfterAppend, schemaMap)
    val (unnest, fieldMapAfterUnnest) = resolveUnnests(query.unnest, fieldMapAfterLookup)

    val (filter, fieldMapAfterFilter) = resolveFilters(query.filter, fieldMapAfterUnnest)

    val (groups, fieldMapAfterGroup) = resolveGroup(query.groups, fieldMapAfterFilter, schemaMap)
    val (select, fieldMapAfterSelect) = resolveSelect(query.select, fieldMapAfterGroup)

    val (globalAggr, fieldMapAfterGlobalAggr) = resolveGlobalAggregate(query.globalAggr, fieldMapAfterSelect)

    Query(query.dataset, append, lookup, filter, unnest, groups, select, globalAggr, query.estimable)
  }

  private def resolveAppends(appends: Seq[UnresolvedAppendStatement],
                             fieldMap: Map[String, Field]): (Seq[AppendStatement], Map[String, Field]) = {
    val producedFields = mutable.Map.newBuilder[String, Field]
    val resolved = appends.map { append =>
      val field = resolveField(append.field, fieldMap)
      val as = Field(append.as, append.resultType)
      producedFields += as.name -> as
      AppendStatement(field, append.definition, as)
    }

    (resolved, (producedFields ++= fieldMap).result().toMap)
  }


  private def resolveLookups(lookups: Seq[UnresolvedLookupStatement],
                             fieldMap: Map[String, Field],
                             schemaMap: Map[String, AbstractSchema]): (Seq[LookupStatement], Map[String, Field]) = {
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

  private def resolveGroup(group: Option[UnresolvedGroupStatement], fieldMap: Map[String, Field], schemaMap: Map[String, AbstractSchema]): (Option[GroupStatement], Map[String, Field]) = {
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

        val (resolvedLookups, newFieldMap) = resolveLookups(groupStatement.lookups, producedFields.result().toMap, schemaMap)

        val resolved = GroupStatement(resolvedBys, resolvedAggrs, resolvedLookups)
        (Some(resolved), newFieldMap)
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
