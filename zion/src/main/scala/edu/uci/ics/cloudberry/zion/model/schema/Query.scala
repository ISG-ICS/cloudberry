package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.datastore.QueryInitException
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation

case class Query(val dataset: String,
                 val lookup: Seq[LookupStatement],
                 val filter: Seq[FilterStatement],
                 val unnest: Seq[UnnestStatement],
                 val groups: Option[GroupStatement],
                 val select: Option[SelectStatement]
                )

case class Append(val dataset: String, val query: Query)

case class Drop(val dataset: String)

trait Statement {
  protected def requireOrThrow(condition: Boolean, msgIfFalse: String): Unit = {
    if (!condition) throw QueryInitException(msgIfFalse)
  }
}

/**
  * Augments the source data to contain more fields.
  *
  * @param sourceKeys
  * @param dataset
  * @param lookupKeys
  * @param selectValues
  * @param as
  */
case class LookupStatement(val sourceKeys: Seq[String],
                           val dataset: String,
                           val lookupKeys: Seq[String],
                           val selectValues: Seq[String],
                           val as: Seq[String]
                          ) extends Statement {
  //TODO to be replaced by a unified syntax exceptions
  requireOrThrow(sourceKeys.length == lookupKeys.length, "LookupStatement: lookup key number is different from size of the source key ")
  requireOrThrow(selectValues.length == as.length, "LookupStatement: select value names doesn't match with renamed names")
}

//TODO only support one transform for now
case class FilterStatement(val fieldName: String,
                           val funcOpt: Option[TransformFunc],
                           val relation: Relation,
                           val values: Seq[Any]
                          ) extends Statement

case class UnnestStatement(val fieldName: String, val as: String)

/**
  * Groupby fieldNames
  *
  * @param fieldName
  * @param funcOpt
  * @param groups //TODO support the auto group by given size
  */
case class ByStatement(val fieldName: String,
                       val funcOpt: Option[GroupFunc],
                       val as: Option[String]
                      ) extends Statement

/**
  * The aggregate results produced by group by
  */
case class AggregateStatement(val fieldName: String,
                              val func: AggregateFunc,
                              val as: String
                             ) extends Statement

case class GroupStatement(val bys: Seq[ByStatement],
                          val aggregates: Seq[AggregateStatement]
                         ) extends Statement {
  requireOrThrow(bys.size > 0, "Group by statement is required")
  requireOrThrow(aggregates.size > 0, "Aggregation statement is required")
}

case class SelectStatement(val orderOn: Seq[String],
                           val limit: Int,
                           val offset: Int,
                           val fields: Seq[String]
                          ) extends Statement {
}


