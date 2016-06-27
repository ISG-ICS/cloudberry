package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat

class AQLVisitor(dataStore: String) extends XQLVisitor {

  import AQLVisitor._

  val aqlBuilder: StringBuilder = StringBuilder.newBuilder

  override def visit(query: DBQuery): Unit = {
    val varName = "t"
    aqlBuilder ++= s"for $$${varName} in dataset ${dataStore}\n"
    aqlBuilder ++= query.predicates.map(p => visitPredicate(varName, p)).mkString("\n")
  }

  def visitPredicate(variable: String, predicate: Predicate): String = {
    predicate match {
      case p: IdSetPredicate =>
        s"""
           |for $$sid in [ ${p.idSets.mkString(",")} ]
           |where $$$variable.${p.fieldName} = $$sid
           |""".stripMargin
      case p: KeywordPredicate =>
        //The first one is utilizing the inverted-index. The rest is using the simple `contains` to save comparison time
        val first =s"""where similarity-jaccard(word-tokens($$$variable."${p.fieldName}"), word-tokens("${p.keywords.head}")) > 0.0"""
        val rest = p.keywords.tail.map(
          keyword => s"""and contains($$$variable."${p.fieldName}", "$keyword")"""
        )
        (first +: rest).mkString("\n")
      case p: TimePredicate =>
        def formatInterval(interval: Interval): String = {
          s"""
             |($$$variable."${p.fieldName}">= datetime("${TimeFormat.print(interval.getStart)}")
             |and $$$variable."${p.fieldName}" <= datetime("${TimeFormat.print(interval.getEnd)}"))
             |""".stripMargin
        }
        s"""
           |where
           |${p.intervals.map(formatInterval).mkString("or")}
           |""".stripMargin
    }
  }

  def visitAggFunc(aggregateOn: AggregateOn): String = {
    import AggFunctionTypes._
    val func = aggregateOn.aggFunction match {
      case Count => "count"
      case Sum => "sum"
    }
    s"""
       |"${aggregateOn.aggFunction.toString}${aggregateOn.fieldName}" : ${func}(${aggregateOn.fieldName})
     """.stripMargin
  }

  def visitGroupby(variable: String, statement: Groupby): String = {
    val groupOn = statement.groupOn.fieldNames.map(name => s""" $$${name} := $$${variable}."${name}" """).mkString(",")
    val aggregate = statement.aggregateOns.map(aggregate =>
                                                 aggregate.fieldName
    )
    //TODO
    s"""
       |group by ${groupOn} with $$${variable}
       |return {
       |
       |}
       """.stripMargin
  }

}

object AQLVisitor {
  def apply(dataStore: String): AQLVisitor = new AQLVisitor(dataStore)

  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

}
