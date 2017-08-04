package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import play.api.libs.ws.WSResponse
import play.api.libs.json.{Json, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Breaks.{break, breakable}
import java.sql.{Connection, DriverManager}
import java.util.Date

import scala.collection.mutable.ListBuffer



class SQLConn(url: String)(implicit ec: ExecutionContext) extends IDataConn {
  val urlConn = getUrlParam("url", url)
  val user = getUrlParam("user", url)
  val passwd = getUrlParam("passwd", url)
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val connection: Connection = DriverManager.getConnection(urlConn, user, passwd)
  val statement = connection.createStatement

  def getUrlParam(param: String, url: String): String = {
    val paramMap: Map[String, String] = url.split("\\?")(1).split("&")
      .map(t => (t.split("=")(0) -> t.split("=")(1))).toMap + ("url" -> url.split("\\?")(0))
    paramMap(param)
  }

  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] = query match {
    case berry if query.contains("`berry.meta`") => postBerryQuery(query)
    case _ => postGeneralQuery(query)
  }

  def postGeneralQuery(query: String): Future[JsValue] = {
    val result = statement.executeQuery(query)
    val resultMetadata = result.getMetaData
    val columnCount = resultMetadata.getColumnCount
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      var columnId = 1
      var rsJson: JsObject = Json.obj()
      while (columnId <= columnCount) {
        val columnLabel = resultMetadata.getColumnLabel(columnId)
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
          case date: Date =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(date.getTime)))
          case long: java.lang.Long =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(long.toLong))
          case double: java.lang.Double =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(double.toDouble))
          case float: java.lang.Float =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(float.asInstanceOf[BigDecimal]))
          case str: String =>
            rsJson = rsJson ++ Json.obj(columnLabel -> JsString(str))
          case _ => breakable {
            break
          }
        }
        columnId += 1
      }
      qJsonArray = qJsonArray :+ rsJson
    }
    Future(Json.toJson(qJsonArray))
  }

  def postBerryQuery(query: String): Future[JsValue] = {
    val result = statement.executeQuery(query)
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      var rsJson: JsObject = Json.obj()
      val name = result.getObject("name")
      val schema = result.getObject("schema")
      val stats = result.getObject("stats")
      val dataInterval = result.getObject("dataInterval")
      rsJson = rsJson ++ Json.obj("name" -> JsString(name.asInstanceOf[String]))
      rsJson = rsJson ++ Json.obj("schema" -> Json.parse(schema.asInstanceOf[String]))
      rsJson = rsJson ++ Json.obj("stats" -> Json.parse(stats.asInstanceOf[String]))
      rsJson = rsJson ++ Json.obj("dataInterval" -> Json.parse(dataInterval.asInstanceOf[String]))
      qJsonArray = qJsonArray :+ rsJson
    }
    Future(Json.toJson(qJsonArray))
  }

  def postControl(query: String) = {
    query.split(";\n").foreach {
      case q => statement.executeUpdate(q)
    }
    Future(true)
  }

}
