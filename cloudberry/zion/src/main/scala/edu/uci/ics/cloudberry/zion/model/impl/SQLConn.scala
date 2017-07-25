package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import java.sql.{Connection, DriverManager}

import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import java.io.InputStream
import java.util.Date

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
  val berryCheck: String = s"""
                              |select name, `schema`
                              |from `berry.meta` t
                              |order by create_at desc
                              |limit 2147483647""".stripMargin

  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(q: String): Future[JsValue] = {
    var query = q
    if (query.contains("berry.meta")) {
      Future(berry)
    } else {
      if (query.contains("like")) {
        val berryResult = statement.executeQuery(berryCheck)
        var done = true
        while (berryResult.next && done) {
          val key: String = berryResult.getString("schema")
          if (query.contains(s"like '%${key}%'")) {
            val name: String = berryResult.getString("name")
            val regex = s"(and lower\\(t\\.text\\) like '%${key}%'|lower\\(t\\.text\\) like '%${key}%' and )".r
            query = regex.replaceAllIn(query, "")
            query = query.replace("twitter_ds_tweet", name)
            done = false
          }
        }
      }

      val result = statement.executeQuery(query)
      val rsmd = result.getMetaData
      val columnCount = rsmd.getColumnCount
      var qJsonArray: JsArray = Json.arr()
      while (result.next) {
        var index = 1
        var rsJson: JsObject = Json.obj()
        while (index <= columnCount) {
          val column = rsmd.getColumnLabel(index)
          val columnLabel = column.toLowerCase()
          val value = result.getObject(column)
          value match {
            case int: Integer =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(int.asInstanceOf[Int]))
            case boolean: java.lang.Boolean =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsBoolean(boolean.asInstanceOf[Boolean]))
            case date: Date =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(date.asInstanceOf[Date].getTime))
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
                case e => rsJson = rsJson ++ Json.obj(columnLabel -> JsString(str.asInstanceOf[String]))
              }
            case any: AnyRef =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNull)
          }
          index += 1
        }
        qJsonArray = qJsonArray :+ rsJson
      }
      Future(Json.toJson(qJsonArray))
    }
  }

  def postControl(query: String) = {
    query.split(";").foreach {
      case q => try {statement.executeUpdate(q)} catch {case e: Exception =>}
    }
    Future(true)
  }

}
