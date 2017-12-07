package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import play.api.libs.json.JsArray

trait IQuery {
  def dataset: String
}

trait IReadQuery extends IQuery {

}

trait IWriteQuery extends IQuery {

}

case class QueryExeOption(sliceMills: Int,
                          continueSeconds: Int,
                          limit: Option[Int])

object QueryExeOption {
  val NoSliceNoContinue = QueryExeOption(-1, -1, None)
  val TagSliceMillis = "sliceMillis"
  val TagContinueSeconds = "continueSeconds"
  val TagLimit = "limit"
}

case class Query(dataset: String,
                 append: Seq[AppendStatement] = Seq.empty,
                 lookup: Seq[LookupStatement] = Seq.empty,
                 filter: Seq[FilterStatement] = Seq.empty,
                 unnest: Seq[UnnestStatement] = Seq.empty,
                 groups: Option[GroupStatement] = None,
                 select: Option[SelectStatement] = None,
                 globalAggr: Option[GlobalAggregateStatement] = None,
                 isEstimable: Boolean = false
                ) extends IReadQuery {

  def hasUnnest: Boolean = !unnest.isEmpty

  def hasGroup: Boolean = groups.isDefined

  def hasSelect: Boolean = select.isDefined

  import TimeField.TimeFormat

  def setInterval(field: Field, interval: org.joda.time.Interval): Query = {
    //TODO support filter query that contains multiple relation on that same field
    val timeFilter = FilterStatement(field, None, Relation.inRange,
      Seq(TimeFormat.print(interval.getStartMillis),
        TimeFormat.print(interval.getEndMillis)))
    this.copy(filter = timeFilter +: this.filter.filterNot(_.field == field))
  }

  def getTimeInterval(field: Field): Option[org.joda.time.Interval] = {
    //TODO support > < etc.
    //TODO support multiple time condition
    filter.find(f => f.field == field && f.relation == Relation.inRange).map { stat =>
      require(stat.values.size == 2)
      val toTime = stat.values.map(v => TimeFormat.parseDateTime(v.asInstanceOf[String]))
      new org.joda.time.Interval(toTime(0), toTime(1))
    }
  }

  def canSolve(another: Query, schema: AbstractSchema): Boolean = {
    //unfiltered field can be covered anyway
    //TODO think another way: just using compare the output schema!!!
    //still need the filter, but won't need to consider the group/select/lookup
    //TODO read paper http://www.vldb.org/conf/1996/P318.PDF
    import Query._

    if (!covers(this.filter, another.filter)) {
      return false
    }

    val isFilterMatch = this.filter.forall(f => another.filter.filter(_.field == f.field)
      .exists(anotherF => f.covers(anotherF)))
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
    thisFilter.forall(f => thatFilter.exists(_.field == f.field))
  }
}

case class CreateView(dataset: String, query: Query) extends IWriteQuery

case class AppendView(dataset: String, query: Query) extends IWriteQuery

case class DropView(dataset: String) extends IWriteQuery

case class CreateDataSet(dataset: String, schema: AbstractSchema, createIffNotExist: Boolean) extends IWriteQuery

case class UpsertRecord(dataset: String, records: JsArray) extends IWriteQuery

case class DeleteRecord(dataset: String, filters: Seq[FilterStatement]) extends IWriteQuery

trait Statement

case class AppendStatement(field: Field,
                           definition: String,
                           as: Field) extends Statement

/**
  * Augments the source data to contain more fields.
  *
  * @param sourceKeys
  * @param dataset
  * @param lookupKeys
  * @param selectValues
  * @param as
  */
case class LookupStatement(sourceKeys: Seq[Field],
                           dataset: String,
                           lookupKeys: Seq[Field],
                           selectValues: Seq[Field],
                           as: Seq[Field]
                          ) extends Statement

//TODO only support at most one transform for now
case class FilterStatement(field: Field,
                           funcOpt: Option[TransformFunc],
                           relation: Relation,
                           values: Seq[Any]
                          ) extends Statement {
  def covers(another: FilterStatement): Boolean = {
    if (field != another.field) {
      false
    } else {
      field.dataType match {
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

case class UnnestStatement(field: Field, as: Field)

/**
  * Groupby fieldNames
  *
  * @param field
  * @param funcOpt
  * *@param groups //TODO support the auto group by given size
  */
case class ByStatement(field: Field,
                       funcOpt: Option[GroupFunc],
                       as: Option[Field]
                      ) extends Statement

/**
  * The aggregate results produced by group by
  */
case class AggregateStatement(field: Field,
                              func: AggregateFunc,
                              as: Field
                             ) extends Statement

case class GroupStatement(bys: Seq[ByStatement],
                          aggregates: Seq[AggregateStatement],
                          lookups: Seq[LookupStatement] = Seq.empty
                         ) extends Statement {
  def finerThan(group: GroupStatement): Boolean = ???
}

case class GlobalAggregateStatement(aggregate: AggregateStatement
                                   ) extends Statement

object SortOrder extends Enumeration {
  val ASC, DSC = Value
}

case class SelectStatement(orderOn: Seq[Field],
                           order: Seq[SortOrder.Value],
                           limit: Int,
                           offset: Int,
                           fields: Seq[Field]
                          ) extends Statement


