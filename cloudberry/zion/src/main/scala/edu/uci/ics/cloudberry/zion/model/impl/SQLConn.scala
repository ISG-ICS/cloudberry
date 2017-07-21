package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import java.sql.{Connection, DriverManager}

import play.api.libs.json._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import java.io.InputStream
import java.util.Date



/**
  * Created by sicongliu on 17/5/19.
  */

class SQLConn(url: String)(implicit ec: ExecutionContext) extends IDataConn{
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

  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] = {
    println("\nSQLConn: postQuery: \n" + query)
    if (query.contains("berry.meta")) {
      println("SQL postQuery: Contains berry.meta")
      Future(berry)
    } else {
      val statement = connection.createStatement
      val result = statement.executeQuery(query)
      val rsmd = result.getMetaData
      val columnCount = rsmd.getColumnCount
      println("SQLConn: columnCount= " + columnCount)
      var qJsonArray: JsArray = Json.arr()
      while (result.next) {
        var index = 1
        var rsJson: JsObject = Json.obj()
        while (index <= columnCount) {
          val column = rsmd.getColumnLabel(index)
          val columnLabel = column.toLowerCase()

          val value = result.getObject(column)
          if (value == null) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> JsNull
            )
          } else if (value.isInstanceOf[Integer]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Int]
            )
          } else if (value.isInstanceOf[String]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[String]
            )
          } else if (value.isInstanceOf[Boolean]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Boolean]
            )
          } else if (value.isInstanceOf[Date]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Date].getTime
            )
          } else if (value.isInstanceOf[Long]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Long]
            )
          } else if (value.isInstanceOf[Double]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Double]
            )
          } else if (value.isInstanceOf[Float]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Float]
            )
          } else if (value.isInstanceOf[BigDecimal]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[BigDecimal]
            )
         } else if (value.isInstanceOf[Array[String]]) {
            rsJson = rsJson ++ Json.obj(
              columnLabel -> value.asInstanceOf[Array[String]]
            )
          } else {
            throw new IllegalArgumentException("Unmappable object type: " + value.getClass)
          }
          index += 1
        }
        qJsonArray = qJsonArray :+ rsJson
      }
      Future(qJsonArray)
    }
  }

  def postControl(query: String) = {
    println(query)
    Future(true)
  }

}
