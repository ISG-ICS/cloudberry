package edu.uci.ics.cloudberry.zion.experimental

import akka.actor.{Actor, ActorLogging}
import edu.uci.ics.cloudberry.zion.asterix.AsterixConnection
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat

import scala.concurrent.ExecutionContext

class DataStoreActor(val name: String, val schema: Schema, val conn: AsterixConnection)
                    (implicit ec: ExecutionContext) extends Actor with ActorLogging {

  import DataStoreActor._

  override def receive: Receive = {
    case query: Query => val thisSender = sender(); conn.post(parseQuery(name, schema, query)).map(thisSender ! _.json)
  }

}

object DataStoreActor {

  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  def visitFilter(builder: StringBuilder, filter: Map[Field, NPredicate]): Unit = {
    val varT = "t"
    builder ++= "\nwhere"
    filter.map { case (field: Field, predicate: NPredicate) =>
      (field.typeValue, predicate) match {
        case (Type.Number, p: NumberPredicate) => s"$$$varT.${p.fieldName} ${p.relation.toString} ${p.value}"
        case (Type.String, p: StringPredicate) => s"$$$varT.${p.fieldName} ${p.relation.toString} (${p.value})"
        case (Type.Text, p: TextPredicate) => s"""similarity-jaccard(word-tokens($$$varT."${p.fieldName}"), word-tokens("${p.value}")) > 0.0""""
        case (Type.GeoPoint, p: GeoPredicate) =>
          s"spatial-intersect($$$varT.${p.fieldName}, " +
            s"create-rectangle(create-point(${p.rectangle.swLog}, ${p.rectangle.swLat}), " +
            s"create-point(${p.rectangle.neLog}, ${p.rectangle.neLat}))"
        case (Type.Time, p: ETimePredicate) =>
          s"""($$$varT.${p.fieldName} >= datetime("${TimeFormat.print(p.interval.getStart)}")
             |and $$$varT.${p.fieldName} < datetime("${TimeFormat.print(p.interval.getEnd)}"))""".stripMargin
        case (Type.ID, p: IdSetPredicate) =>
          s"""let $$set := [ ${p.idSets.mkString(",")} ]
           |for $$sid in $$set
           """
        case (Type.Nested, p: Any) =>
      }
    }.mkString("\n and ")
  }

  def visitDimension(builder: StringBuilder, dimensions: Seq[Dimension]) = ???

  def visitMeasure(builder: StringBuilder, measurements: Seq[Measurement]) = ???

  def parseQuery(dataset: String, schema: Schema, query: Query): String = {
    val builder = StringBuilder.newBuilder
    builder ++= s"for $$t in dataset $dataset"
    visitFilter(builder, query.filters.filter(p => schema.field(p.fieldName).isDefined).map(p => schema.field(p).get -> p).toMap)
    visitDimension(builder, query.dimensions)
    visitMeasure(builder, query.measurements)
    ???
  }
}
