package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.datastore.QueryInitException
import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import play.api.libs.json.JsArray

trait IQuery {
  def dataset: String
}

case class Query(dataset: String,
                 lookup: Seq[LookupStatement] = Seq.empty,
                 filter: Seq[FilterStatement] = Seq.empty,
                 unnest: Seq[UnnestStatement] = Seq.empty,
                 groups: Option[GroupStatement] = None,
                 select: Option[SelectStatement] = None,
                 globalAggr: Option[GlobalAggregateStatement] = None
                ) extends IQuery {

  import TimeField.TimeFormat

  def setInterval(fieldName: String, interval: org.joda.time.Interval): Query = {
    //TODO support filter query that contains multiple relation on that same field
    val timeFilter = FilterStatement(fieldName, None, Relation.inRange,
                                     Seq(TimeFormat.print(interval.getStartMillis),
                                         TimeFormat.print(interval.getEndMillis)))
    this.copy(filter = timeFilter +: this.filter.filterNot(_.fieldName == fieldName))
  }

  def getTimeInterval(fieldName: String): Option[org.joda.time.Interval] = {
    //TODO support > < etc.
    //TODO support multiple time condition
    filter.find(f => f.fieldName == fieldName && f.relation == Relation.inRange).map { stat =>
      require(stat.values.size == 2)
      val toTime = stat.values.map(v => TimeFormat.parseDateTime(v.asInstanceOf[String]))
      new org.joda.time.Interval(toTime(0), toTime(1))
    }
  }

  def canSolve(another: Query, schema: Schema): Boolean = {
    //unfiltered field can be covered anyway
    //TODO think another way: just using compare the output schema!!!
    //still need the filter, but won't need to consider the group/select/lookup
    //TODO read paper http://www.vldb.org/conf/1996/P318.PDF
    import Query._

    if (!covers(this.filter, another.filter)) {
      return false
    }

    val isFilterMatch = this.filter.forall(f => another.filter.filter(_.fieldName == f.fieldName)
      .exists(anotherF => f.covers(anotherF, schema.fieldMap(f.fieldName).dataType)))
    if (!isFilterMatch) {
      return false
    }

    val isGroupMatch = another.groups match {
      case None => this.groups.isEmpty
      case Some(group) => this.groups.forall(_.finerThan(group))
    }

    isGroupMatch && this.unnest.isEmpty && this.select.isEmpty
  }

}

object Query {

  def covers(thisFilter: Seq[FilterStatement], thatFilter: Seq[FilterStatement]): Boolean = {
    thisFilter.forall(f => thatFilter.exists(_.fieldName == f.fieldName))
  }
}


case class CreateView(dataset: String, query: Query) extends IQuery

case class AppendView(dataset: String, query: Query) extends IQuery

case class DropView(dataset: String) extends IQuery

case class CreateDataSet(dataset: String, schema: Schema, createIffNotExist: Boolean) extends IQuery

case class UpsertRecord(dataset: String, records: JsArray) extends IQuery

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

//TODO only support at most one transform for now
case class FilterStatement(fieldName: String,
                           funcOpt: Option[TransformFunc],
                           relation: Relation,
                           values: Seq[Any]
                          ) extends Statement {
  def covers(another: FilterStatement, dataType: DataType): Boolean = {
    if (fieldName != another.fieldName) {
      false
    } else {
      dataType match {
        case DataType.Number => ???
        case DataType.Time => ???
        case DataType.Point => ???
        case DataType.Boolean => ???
        case DataType.String => ???
        case DataType.Text => relation == another.relation && values.forall(v => another.values.contains(v))
        case DataType.Bag => ???
        case DataType.Hierarchy => ???
        case DataType.Record => ???
      }
    }
  }
}

case class UnnestStatement(fieldName: String, as: String)

/**
  * Groupby fieldNames
  *
  * @param fieldName
  * @param funcOpt
  * *@param groups //TODO support the auto group by given size
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

  requireOrThrow(bys.nonEmpty, "By statement is required")
  requireOrThrow(aggregates.nonEmpty, "Aggregation statement is required")
}

case class GlobalAggregateStatement(aggregate: AggregateStatement
                                   ) extends Statement {
}

case class SelectStatement(orderOn: Seq[String],
                           limit: Int,
                           offset: Int,
                           fields: Seq[String]
                          ) extends Statement {
}


