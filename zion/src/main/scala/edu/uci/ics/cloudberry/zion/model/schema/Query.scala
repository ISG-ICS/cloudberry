package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.datastore.QueryInitException
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation

trait IQuery {
  def dataset: String
}

case class Query(dataset: String,
                 lookup: Seq[LookupStatement],
                 filter: Seq[FilterStatement],
                 unnest: Seq[UnnestStatement],
                 groups: Option[GroupStatement],
                 select: Option[SelectStatement]
                ) extends IQuery {
  def replaceInterval(interval: org.joda.time.Interval): Query = ???

  def getTimeInterval: org.joda.time.Interval = ???
}


case class CreateView(dataset: String, query: Query) extends IQuery

case class AppendView(dataset: String, interval: org.joda.time.Interval, query: Query) extends IQuery

case class DropView(dataset: String) extends IQuery

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
case class LookupStatement(sourceKeys: Seq[String],
                           dataset: String,
                           lookupKeys: Seq[String],
                           selectValues: Seq[String],
                           as: Seq[String]
                          ) extends Statement {
  //TODO to be replaced by a unified syntax exceptions
  requireOrThrow(sourceKeys.length == lookupKeys.length, "LookupStatement: lookup key number is different from size of the source key ")
  requireOrThrow(selectValues.length == as.length, "LookupStatement: select value names doesn't match with renamed names")
}

//TODO only support one transform for now
case class FilterStatement(fieldName: String,
                           funcOpt: Option[TransformFunc],
                           relation: Relation,
                           values: Seq[Any]
                          ) extends Statement {
  def include(another: FilterStatement): Boolean = ???
}

case class UnnestStatement(fieldName: String, as: String)

/**
  * Groupby fieldNames
  *
  * @param fieldName
  * @param funcOpt
  * @param groups //TODO support the auto group by given size
  */
case class ByStatement(fieldName: String,
                       funcOpt: Option[GroupFunc],
                       as: Option[String]
                      ) extends Statement

/**
  * The aggregate results produced by group by
  */
case class AggregateStatement(fieldName: String,
                              func: AggregateFunc,
                              as: String
                             ) extends Statement

case class GroupStatement(bys: Seq[ByStatement],
                          aggregates: Seq[AggregateStatement]
                         ) extends Statement {
  def finerThan(group: GroupStatement): Boolean = ???

  requireOrThrow(bys.nonEmpty, "Group by statement is required")
  requireOrThrow(aggregates.nonEmpty, "Aggregation statement is required")
}

case class SelectStatement(orderOn: Seq[String],
                           limit: Int,
                           offset: Int,
                           fields: Seq[String]
                          ) extends Statement {
}


