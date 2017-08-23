package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import play.api.libs.ws.WSResponse
import play.api.libs.json.{Json, _}
import play.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Breaks.{break, breakable}
import java.sql.{Connection, DriverManager, _}
import java.lang._


class MySQLConn(url: String)(implicit ec: ExecutionContext) extends IDataConn {
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val connection: Connection = DriverManager.getConnection(url)

  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] = query match {
    case berry if query.contains(SQLConn.metaName) => postBerryQuery(query)
    case _ => postGeneralQuery(query)
  }

  protected def postGeneralQuery(query: String): Future[JsValue] = {
    val statement = connection.createStatement
    val result = statement.executeQuery(query)
    val resultMetadata = result.getMetaData
    val columnCount = resultMetadata.getColumnCount
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      var columnId = 0
      var rsJson: JsObject = Json.obj()
      breakable {
        for (columnId <- 1 to columnCount) {
          val columnLabel = resultMetadata.getColumnLabel(columnId)
          val value = result.getObject(columnLabel)
          value match {
            case int: Integer =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(int.toInt))
            case boolean: java.lang.Boolean =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsBoolean(boolean))
            case date: Date =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(date.getTime)))
            case time: Time =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(time.getTime)))
            case timestamp: Timestamp =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(timestamp.getTime)))
            case long: Long =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(long.toLong))
            case double: Double =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(double.toDouble))
            case float: Float =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(float.asInstanceOf[BigDecimal]))
            case short: Short =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(short.toInt))
            case decimal: BigDecimal =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(decimal))
            case str: String =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(str))
            case blob: Blob => //large data
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(blob.toString))
            case byte: Byte =>
              rsJson = rsJson ++ Json.obj(columnLabel -> byte.toByte)
            case _ =>
              Logger.warn(s"type of value $value is not detectd")
              break
          }
        }
        qJsonArray = qJsonArray :+ rsJson
      }
    }
    Future(Json.toJson(qJsonArray))
  }

  protected def postBerryQuery(query: String): Future[JsValue] = {
    val statement = connection.createStatement
    val result = statement.executeQuery(query)
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      var rsJson: JsObject = Json.obj()
      val name = result.getObject("name")
      val schema = result.getObject("schema")
      val stats = result.getObject("stats")
      val dataInterval = result.getObject("dataInterval")
      rsJson = rsJson ++ Json.obj("name" -> JsString(name.asInstanceOf[String]))
      rsJson = rsJson ++ Json.obj("schema" -> Json.parse(schema.toString))
      rsJson = rsJson ++ Json.obj("stats" -> Json.parse(stats.toString))
      rsJson = rsJson ++ Json.obj("dataInterval" -> Json.parse(dataInterval.toString))
      qJsonArray = qJsonArray :+ rsJson
    }
    Future(Json.toJson(qJsonArray))
  }

  def postControl(query: String) = {
    val statement = connection.createStatement
    query.split(";\n").foreach {
      case q => statement.executeUpdate(q)
    }
    Future(true)
  }

}

object SQLConn {
  val metaName = "berry.meta"
}
