package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import java.sql.{Connection, DriverManager}

import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import java.io.InputStream
import java.util.Date
import java.text.SimpleDateFormat

class SQLConn(url: String, wSClient: WSClient)(implicit ec: ExecutionContext) extends IDataConn {
  val driver = "com.mysql.jdbc.Driver"
  val username = "root"
  val password = "2048"
  var connection:Connection = _
  Class.forName(driver)
  connection = DriverManager.getConnection(url, username, password)
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val stream : InputStream = getClass.getResourceAsStream("/ddl/berry.json")
  val source = scala.io.Source.fromInputStream(stream).getLines.mkString
  val berry: JsValue = Json.parse(source)
  val statement = connection.createStatement

  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] = {
      val result = statement.executeQuery(query)
      val rsmd = result.getMetaData
      val columnCount = rsmd.getColumnCount
      var qJsonArray: JsArray = Json.arr()
      while (result.next) {
        var index = 1
        var rsJson: JsObject = Json.obj()
        while (index <= columnCount) {
          val column = rsmd.getColumnLabel(index)
          val columnLabel = column
          val value = result.getObject(column)
          value match {
            case int: Integer =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(int.asInstanceOf[Int]))
            case boolean: java.lang.Boolean =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsBoolean(boolean.asInstanceOf[Boolean]))
            case date: Date =>
              val minuteFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(minuteFormat.format(date.asInstanceOf[Date].getTime)))
            case long: java.lang.Long =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(long.asInstanceOf[Long]))
            case double: java.lang.Double =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(double.asInstanceOf[Double]))
            case float: java.lang.Float =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(float.asInstanceOf[BigDecimal]))
            case arr: Array[String] =>
              rsJson = rsJson ++ Json.obj(columnLabel -> Json.toJson(arr.asInstanceOf[Array[String]]))
            case str: String =>
              try {
                rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(str.toInt))
              } catch {
                case e =>
                  try {
                    rsJson = rsJson ++ Json.obj(columnLabel -> Json.parse(str))
                  } catch {
                    case other => rsJson = rsJson ++ Json.obj(columnLabel -> JsString(str.asInstanceOf[String]))
                  }
              }
            case any: AnyRef =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNull)
          }
          index += 1
        }
        qJsonArray = qJsonArray :+ rsJson
      }
      if (qJsonArray == JsArray() && query.contains("berry.meta")) {
        Future(berry)
      } else {
        Future(Json.toJson(qJsonArray))
      }
  }

  def postControl(query: String) = {
    query.split(";").foreach {
      case q => statement.executeUpdate(q)
    }
    Future(true)
  }

}
