package edu.uci.ics.cloudberry.zion.model.impl

import java.security.MessageDigest

import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue}

class QueryPlanner {

  import QueryPlanner._

  def makePlan(query: Query, source: DataSetInfo, views: Seq[DataSetInfo]): (Seq[Query], (TraversableOnce[JsValue] => JsValue)) = {

    val matchedViews = views.filter(view => view.createQueryOpt.exists(vq => vq.canSolve(query, source.schema)))
    //TODO currently only get the best one
    val bestView = selectBestView(matchedViews)
    splitQuery(query, source, bestView)
  }

  def suggestNewView(query: Query, source: DataSetInfo, views: Seq[DataSetInfo]): Seq[CreateView] = {
    //TODO currently only suggest the keyword subset views
    if (views.exists(v => v.createQueryOpt.exists(vq => vq.canSolve(query, source.schema)))) {
      Seq.empty[CreateView]
    } else {
      val keywordFilters = query.filter.filter(f => source.schema.fieldMap(f.fieldName).dataType == DataType.Text)
      keywordFilters.flatMap { kwFilter =>
        kwFilter.values.map { wordAny =>
          val word = wordAny.asInstanceOf[String]
          val wordFilter = FilterStatement(kwFilter.fieldName, None, Relation.contains, Seq(word))
          val wordQuery = Query(query.dataset, Seq.empty, Seq(wordFilter), Seq.empty, None, None)
          CreateView(getViewKey(query.dataset, word), wordQuery)
        }
      }
    }
  }

  private def selectBestView(matchedViews: Seq[DataSetInfo]): Option[DataSetInfo] = {
    if (matchedViews.isEmpty) {
      None
    } else {
      Some(matchedViews.min(Ordering.by((info: DataSetInfo) => info.stats.cardinality)))
    }
  }


  private def splitQuery(query: Query, source: DataSetInfo, bestView: Option[DataSetInfo]): (Seq[Query], (TraversableOnce[JsValue] => JsValue)) = {
    bestView match {
      case None => (Seq(query), (jsons: TraversableOnce[JsValue]) => jsons.toList.head)
      case Some(view) =>
        val queryInterval = query.getTimeInterval(source.schema.timeField).getOrElse(new Interval(source.stats.createTime, DateTime.now()))
        val viewInterval = new Interval(source.stats.createTime, view.stats.lastModifyTime)
        val unCovered = getUnCoveredInterval(viewInterval, queryInterval)

        val seqBuilder = Seq.newBuilder[Query]

        //TODO here is a very simple assumption that the schema is the same, what if the schema are different?
        seqBuilder += query.copy(dataset = view.name)
        for (interval <- unCovered) {
          seqBuilder += query.setInterval(source.schema.timeField, interval)
        }
        (seqBuilder.result(), calculateMergeFunc(query, source.schema))
    }
  }

  private def calculateMergeFunc(query: Query, schema: Schema): (TraversableOnce[JsValue] => JsValue) = {
    //TODO the current logic is very simple, all queries has to be the isomorphism.
    //Furthermore the merge type has to be unified.
    if (query.globalAggr.isDefined) {
      ???
    }
    if (query.lookup.nonEmpty) {
      ???
    }

    //TODO very shitty code. clear it!
    var mergeType = MergeType.Distinct
    query.groups match {
      case Some(groupStatement) => {
        val keys = groupStatement.bys.map(by => by.as.getOrElse(by.fieldName))
        val values = groupStatement.aggregates.map(aggr => aggr.as)
        mergeType = groupStatement.aggregates.map { stat: AggregateStatement =>
          stat.func match {
            case Count => MergeType.Sum
            case Sum => MergeType.Sum
            case Max => ???
            case Min => ???
            case Avg => ???
            case topK: TopK => ???
            case _ => ???
          }
        }.head

        query.select match {
          case Some(selectStatement) =>
            if (selectStatement.fields.nonEmpty) {
              ???
            }
            if (selectStatement.orderOn.forall(o => values.contains(o.stripPrefix("-")))) {
              val postProcess = PostProcess(selectStatement.orderOn.map { f =>
                val id = values.indexOf(f.stripPrefix("-"))
                if (f.startsWith("-")) -id else id
              }, selectStatement.limit)
              return (jsons: TraversableOnce[JsValue]) =>
                merge(jsons.toSeq.map(_.asInstanceOf[JsArray]), keys, values, mergeType, Some(postProcess))
            }
            else {
              ???
            }
          case None =>
            return (jsons: TraversableOnce[JsValue]) =>
              merge(jsons.toSeq.map(_.asInstanceOf[JsArray]), keys, values, mergeType, None)
        }
      }
      case None =>
        query.select match {
          case Some(selectStatement) =>
            val values = selectStatement.orderOn.map(_.stripPrefix("-"))
            val keys = selectStatement.fields.filterNot(values.contains(_))
            val postProcess = PostProcess(selectStatement.orderOn.map { f =>
              val id = values.indexOf(f.stripPrefix("-"))
              if (f.startsWith("-")) -id else id
            }, selectStatement.limit)

            return (jsons: TraversableOnce[JsValue]) =>
              merge(jsons.toSeq.map(_.asInstanceOf[JsArray]), keys, values, mergeType, Some(postProcess))
          case None => ???
        }
    }
  }

}

object QueryPlanner {

  def getUnCoveredInterval(dataInterval: Interval, queryInterval: Interval): Seq[Interval] = {
    val intersect = dataInterval.overlap(queryInterval)
    if (intersect == null) {
      return Seq(queryInterval)
    }
    val intervals = scala.collection.mutable.ArrayBuffer.empty[Interval]
    if (queryInterval.getStartMillis < intersect.getStartMillis) {
      intervals += new Interval(queryInterval.getStartMillis, intersect.getStartMillis)
    }
    if (intersect.getEndMillis < queryInterval.getEndMillis) {
      intervals += new Interval(intersect.getEndMillis, queryInterval.getEndMillis)
    }
    intervals
  }

  def getViewKey(sourceName: String, keyword: String): String = {
    sourceName + "_" + MessageDigest.getInstance("MD5").digest(keyword.getBytes("UTF-8")).map("%02x" format _).mkString
  }

  def unionAll(responses: TraversableOnce[JsValue]): JsArray = {
    val builder = Seq.newBuilder[JsValue]
    responses.foreach { jsValue =>
      builder ++= jsValue.asInstanceOf[JsArray].value
    }
    JsArray(builder.result())
  }

  object MergeType extends Enumeration {
    val Distinct, Sum = Value
  }

  case class PostProcess(orders: Seq[Int], limit: Int)

  def compareWithIdx(seqLeft: Seq[JsValue], seqRight: Seq[JsValue], idx: Seq[Int]): Boolean = {
    idx.foreach { idOptNeg =>
      val id = if (idOptNeg < 0) -idOptNeg else idOptNeg
      if (seqLeft(id).asInstanceOf[JsNumber].value < seqRight(id).asInstanceOf[JsNumber].value) {
        return if (idOptNeg >= 0) true else false
      }
      if (seqLeft(id).asInstanceOf[JsNumber].value > seqRight(id).asInstanceOf[JsNumber].value) {
        return if (idOptNeg >= 0) false else true
      }
    }
    true
  }

  def merge(jsons: Seq[JsArray],
            keys: Seq[String],
            values: Seq[String],
            mergeType: MergeType.Value,
            postProcessOpt: Option[PostProcess]
           ): JsArray = {
    if (jsons.isEmpty) return JsArray()

    val map = scala.collection.mutable.Map[Seq[JsValue], Seq[JsValue]]()
    map ++= jsons.head.value.map { r =>
      val k = keys.map(r \ _).map(_.get)
      val v = values.map(r \ _).map(_.get)
      k -> v
    }

    for (json <- jsons.tail) {
      for (record <- json.value) {
        val ks = keys.map(record \ _).map(_.get)
        val vs = values.map(record \ _).map(_.get)

        map.get(ks) match {
          case Some(oldVs) =>
            mergeType match {
              case MergeType.Sum =>
                oldVs.zip(vs).map { case (oldV, newV) =>
                  JsNumber(oldV.asInstanceOf[JsNumber].value + newV.asInstanceOf[JsNumber].value)
                }
              case MergeType.Distinct =>
              // skip it
            }
          case None => map += ks -> vs
        }
      }
    }
    val seq = postProcessOpt match {
      case Some(process) => map.toSeq.sortWith { (left, right) =>
        compareWithIdx(left._2, right._2, process.orders)
      }.slice(0, process.limit)
      case None => map.toSeq
    }
    JsArray(seq.map { case (ks, vs) =>
      JsObject(
        keys.zip(ks).map(pair => pair._1 -> pair._2) ++
          values.zip(vs).map(pair => pair._1 -> pair._2)
      )
    })
  }
}
