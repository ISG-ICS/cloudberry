package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{FieldNotFound, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.Interval


/**
  * Unresolved versions of Query classes.
  * Served as the initial parse results of [[JSONParser]], which are resolved by [[QueryResolver]]
  */
object Unresolved {
  def toUnresolved(dataSetInfo: DataSetInfo): UnresolvedDataSetInfo =
    UnresolvedDataSetInfo(
      dataSetInfo.name,
      dataSetInfo.createQueryOpt.map(toUnresolved(_)),
      toUnresolved(dataSetInfo.schema),
      dataSetInfo.dataInterval,
      dataSetInfo.stats
    )

  def toUnresolved(schema: AbstractSchema): UnresolvedSchema = {
    schema.toUnresolved
  }

  def toUnresolved(query: Query): UnresolvedQuery =
    UnresolvedQuery(
      query.dataset,
      query.append.map(toUnresolved(_)),
      query.lookup.map(toUnresolved(_)),
      query.filter.map(toUnresolved(_)),
      query.unnest.map(toUnresolved(_)),
      query.groups.map(toUnresolved(_)),
      query.select.map(toUnresolved(_)),
      query.globalAggr.map(toUnresolved(_)),
      query.isEstimable
    )

  def toUnresolved(append: AppendStatement): UnresolvedAppendStatement = {
    UnresolvedAppendStatement(
      append.field.name,
      append.definition,
      append.as.dataType,
      append.as.name
    )
  }

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
      group.aggregates.map(toUnresolved(_)),
      group.lookups.map(toUnresolved(_))
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


/**
  * This class is an unresolved version of [[AbstractSchema]].
  * The differences are
  *   [[primaryKey]] is string,
  *   [[timeField]] is Option[String] corresponding to temporal schema and static schema.
  * which are resolved later into [[Field]] and to [[Schema]] or [[LookupSchema]].
  */
case class UnresolvedSchema(typeName: String,
                            dimension: Seq[Field],
                            measurement: Seq[Field],
                            primaryKey: Seq[String],
                            timeField: Option[String]
                           ) {
  private lazy val fields = dimension ++ measurement

  def getField(field: String): Option[Field] = {
    fields.find(_.name == field.trim) match {
      case some: Some[Field] => some
      case None => None
    }
  }

  def toResolved: AbstractSchema = {
    val resolvedPrimaryKey = primaryKey.map(this.getField(_).get)
    timeField match {
      case Some(field) =>
        val resolvedTimeField = this.getField(field) match {
          case Some(f) =>
            if(f.isInstanceOf[TimeField]){
              f.asInstanceOf[TimeField]
            } else {
              throw new QueryParsingException("Specified timeField " + field + " of schema " + typeName + " is not in TimeField format.")
            }
          case None =>
            throw FieldNotFound(field)
        }
        Schema(typeName, dimension, measurement, resolvedPrimaryKey, resolvedTimeField)
      case None =>
        LookupSchema(typeName, dimension, measurement, resolvedPrimaryKey)
    }
  }
}

/**
  * This class is an unresolved version of [[DataSetInfo]].
  * The difference is that [[createQueryOpt]] and [[schema]] are [[UnresolvedQuery]] and [[UnresolvedSchema]],
  * which are resolved later into [[Query]] and [[AbstractSchema]]
  */
case class UnresolvedDataSetInfo(name: String,
                                 createQueryOpt: Option[UnresolvedQuery],
                                 schema: UnresolvedSchema,
                                 dataInterval: Interval,
                                 stats: Stats)

case class UnresolvedQuery(dataset: String,
                           append: Seq[UnresolvedAppendStatement] = Seq.empty,
                           lookup: Seq[UnresolvedLookupStatement] = Seq.empty,
                           filter: Seq[UnresolvedFilterStatement] = Seq.empty,
                           unnest: Seq[UnresolvedUnnestStatement] = Seq.empty,
                           groups: Option[UnresolvedGroupStatement] = None,
                           select: Option[UnresolvedSelectStatement] = None,
                           globalAggr: Option[UnresolvedGlobalAggregateStatement] = None,
                           estimable: Boolean = false
                          ) extends IReadQuery

case class UnresolvedAppendStatement(field: String,
                                     definition: String,
                                     resultType: DataType.DataType,
                                     as: String) extends Statement


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
                                    aggregates: Seq[UnresolvedAggregateStatement],
                                    lookups: Seq[UnresolvedLookupStatement]
                                   ) extends Statement

case class UnresolvedGlobalAggregateStatement(aggregate: UnresolvedAggregateStatement
                                             ) extends Statement

case class UnresolvedSelectStatement(orderOn: Seq[String],
                                     limit: Int,
                                     offset: Int,
                                     fields: Seq[String]
                                    ) extends Statement
