package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.datastore.QueryInitException
import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import play.api.libs.json.JsArray

trait IQuery {
  def datasetName: String

  def dataset: Schema
}

case class QueryExeOption(sliceMills: Int, continueSeconds: Int)

object QueryExeOption {
  val NoSliceNoContinue = QueryExeOption(-1, -1)
  val TagSliceMillis = "sliceMillis"
  val TagContinueSeconds = "continueSeconds"
}

case class Query(datasetName: String,
                 lookups: Seq[LookupStatement] = Seq.empty,
                 filters: Seq[FilterStatement] = Seq.empty,
                 unnests: Seq[UnnestStatement] = Seq.empty,
                 group: Option[GroupStatement] = None,
                 select: Option[SelectStatement] = None,
                 globalAggr: Option[GlobalAggregateStatement] = None) extends IQuery {

  import TimeField.TimeFormat

  var dataset: Schema = null

  def setInterval(fieldName: String, interval: org.joda.time.Interval): Query = {
    //TODO support filter query that contains multiple relation on that same field
    val timeFilter = FilterStatement(fieldName, None, Relation.inRange,
      Seq(TimeFormat.print(interval.getStartMillis),
        TimeFormat.print(interval.getEndMillis)))
    this.copy(filters = timeFilter +: this.filters.filterNot(_.fieldName == fieldName))
  }

  def getTimeInterval(fieldName: String): Option[org.joda.time.Interval] = {
    //TODO support > < etc.
    //TODO support multiple time condition
    filters.find(f => f.fieldName == fieldName && f.relation == Relation.inRange).map { stat =>
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

    if (!covers(this.filters, another.filters)) {
      return false
    }

    val isFilterMatch = this.filters.forall(f => another.filters.filter(_.fieldName == f.fieldName)
      .exists(anotherF => f.covers(anotherF, schema.fieldMap(f.fieldName).dataType)))
    if (!isFilterMatch) {
      return false
    }

    val isGroupMatch = another.group match {
      case None => this.group.isEmpty
      case Some(group) => this.group.forall(_.finerThan(group))
    }

    isGroupMatch && this.unnests.isEmpty && this.select.isEmpty
  }

}

object Query {

  def covers(thisFilter: Seq[FilterStatement], thatFilter: Seq[FilterStatement]): Boolean = {
    thisFilter.forall(f => thatFilter.exists(_.fieldName == f.fieldName))
  }
}

case class CreateView(datasetName: String, query: Query) extends IQuery {
  var dataset: Schema = null
}

case class AppendView(datasetName: String, query: Query) extends IQuery {
  var dataset: Schema = null
}

case class DropView(datasetName: String) extends IQuery {
  var dataset: Schema = null
}

case class CreateDataSet(datasetName: String, schema: Schema, createIffNotExist: Boolean) extends IQuery {
  var dataset: Schema = null
}

case class UpsertRecord(datasetName: String, records: JsArray) extends IQuery {
  var dataset: Schema = null
}

trait Statement {
  protected def requireOrThrow(condition: Boolean, msgIfFalse: String): Unit = {
    if (!condition) throw QueryInitException(msgIfFalse)
  }
}

/**
  * Augments the source data to contain more fields.
  *
  * @param sourceKeys
  * @param datasetName
  * @param lookupKeys
  * @param selectValues
  * @param as
  */
case class LookupStatement(sourceKeys: Seq[String],
                           datasetName: String,
                           lookupKeys: Seq[String],
                           selectValues: Seq[String],
                           as: Seq[String]) extends Statement {

  var sourceKeyFields: Seq[Field] = null
  var lookupKeyFields: Seq[Field] = null
  var selectValueFields: Seq[Field] = null
  var asFields: Seq[Field] = null

  //TODO to be replaced by a unified syntax exceptions
  requireOrThrow(sourceKeys.length == lookupKeys.length, "LookupStatement: lookup key number is different from size of the source key ")
  requireOrThrow(selectValues.length == as.length, "LookupStatement: select value names doesn't match with renamed names")
}

//TODO only support at most one transform for now
case class FilterStatement(fieldName: String,
                           funcOpt: Option[TransformFunc],
                           relation: Relation,
                           values: Seq[Any]) extends Statement {

  var field: Field = null

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

case class UnnestStatement(fieldName: String, as: String) {
  var field: Field = null
  var asField: Field = null
}

/**
  * Groupby fieldNames
  *
  * @param fieldName
  * @param funcOpt
  * *@param groups //TODO support the auto group by given size
  */
case class ByStatement(fieldName: String,
                       funcOpt: Option[GroupFunc],
                       as: Option[String]) extends Statement {
  var field: Field = null
  var asField: Option[Field] = null
}

/**
  * The aggregate results produced by group by
  */
case class AggregateStatement(fieldName: String,
                              func: AggregateFunc,
                              as: String) extends Statement {
  var field: Field = null
  var asField: Field = null

}

case class GroupStatement(bys: Seq[ByStatement],
                          aggregates: Seq[AggregateStatement]) extends Statement {
  def finerThan(group: GroupStatement): Boolean = ???

  requireOrThrow(bys.nonEmpty, "By statement is required")
  requireOrThrow(aggregates.nonEmpty, "Aggregation statement is required")
}

case class GlobalAggregateStatement(aggregate: AggregateStatement) extends Statement {
}

case class SelectStatement(orderOn: Seq[String],
                           limit: Int,
                           offset: Int,
                           fieldNames: Seq[String]) extends Statement {
  var orderOnFields: Seq[Field] = null
  var fields: Seq[Field] = null

}


