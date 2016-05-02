package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api._
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat

class AQLVisitor(dataStore: DataStore) extends XQLVisitor {

  val aqlBuilder: StringBuilder = StringBuilder.newBuilder

  override def visit(query: DBQuery): Unit = {
    val varName = "t"
    aqlBuilder ++= s"for $$${varName} in dataset ${dataStore.name}\n"
    aqlBuilder ++= query.predicates.map(p => visitPredicate(varName, p)).mkString("\n")
  }

  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  def visitPredicate(variable: String, predicate: Predicate): String = {
    predicate match {
      case p: KeywordPredicate =>
        p.keywords.map(keyword =>
                         s"""
                            |where similarity-jaccard(word-tokens($${$variable}."${p.fieldName}"), word-tokens("${keyword}")) > 0.0
                            |""".stripMargin
        ).mkString("\n")
      case p: TimePredicate =>
        def formatInterval(interval: Interval): String = {
          s"""
             |($${$variable}."${p.fieldName}">= datetime("${TimeFormat.print(interval.getStart)}")
             |and $${$variable}."${p.fieldName}" < datetime("${TimeFormat.print(interval.getEnd)}"))
           """.stripMargin
        }
        s"""
           |where
           |${p.intervals.map(formatInterval).mkString(" or " )}
         """.stripMargin
        s"""
           |where
     """.stripMargin
      case p: IdSetPredicate =>
        s"""
           |let $$set := [ ${p.idSets.mkString(",")} ]
           |for $$sid in $$set
           |where $${$variable}."${p.fieldName}" = $$sid
         """.stripMargin
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
    s"""
       |group by ${groupOn} with $$${variable}
       |return {
       |
       |}
       """.stripMargin
  }

}

object AQLVisitor {
  def apply(dataStore: DataStore): AQLVisitor = new AQLVisitor(dataStore)
}
