package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import play.api.libs.ws.WSResponse
import play.api.libs.json.{Json, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Breaks.{break, breakable}
import java.sql.{Connection, DriverManager}
import java.io.InputStream
import java.util.Date

import java.text.SimpleDateFormat


class SQLConn(url: String)(implicit ec: ExecutionContext) extends IDataConn {
  val driver = "com.mysql.jdbc.Driver"
  val urlConn: String = url.split("\\?")(0)
  val param: Map[String, String] = url.split("\\?")(1).split("&").map(t => (t.split("=")(0) -> t.split("=")(1))).toMap
  val user = param("user")
  val passwd = param("passwd")
  Class.forName(driver)
  val connection: Connection = DriverManager.getConnection(urlConn, user, passwd)
  val stream: InputStream = getClass.getResourceAsStream("/ddl/berry.json")
  val source = scala.io.Source.fromInputStream(stream).getLines.mkString
  val berry: JsValue = Json.parse(source)
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val statement = connection.createStatement

  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] = {
    val result = statement.executeQuery(query)
    val resultMetadata = result.getMetaData
    val columnCount = resultMetadata.getColumnCount
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      var index = 1
      var rsJson: JsObject = Json.obj()
      while (index <= columnCount) {
        val columnLabel = resultMetadata.getColumnLabel(index)
        val value = result.getObject(columnLabel)
        breakable {
          if (value == null) {
            break
          }
        }
        value match {
          case int: Integer =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(int.toInt))
          case boolean: java.lang.Boolean =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsBoolean(boolean))
          case date: Date =>  //TODO: java.sql.Date cannot be cast to org.joda.time.DateTime
            val minuteFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            rsJson = rsJson ++ Json.obj(columnLabel -> JsString(minuteFormat.format(date.getTime)))
          case long: java.lang.Long =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(long.toLong))
          case double: java.lang.Double =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(double.toDouble))
          case float: java.lang.Float =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(float.asInstanceOf[BigDecimal]))
          case str: String =>
            try {
              rsJson = rsJson ++ Json.obj(columnLabel -> Json.parse(str))
            } catch {
              case string =>
                rsJson = rsJson ++ Json.obj(columnLabel -> JsString(str))
            }
          case _ => breakable {
            break
          }
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
    query.split(";\n").foreach {
      case q => statement.executeUpdate(q)
    }
    Future(true)
  }

}
