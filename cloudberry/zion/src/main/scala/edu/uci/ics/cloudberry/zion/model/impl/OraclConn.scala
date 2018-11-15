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
import java.net.URL
import oracle.sql._


class OracleConn(url: String)(implicit ec: ExecutionContext) extends IDataConn {
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val connection: Connection = DriverManager.getConnection(url)



  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] =
    query match {
      case berry if query.contains(OracleConn.metaName)  => postBerryQuery(query)
      case _ => postGeneralQuery(query)
    }

  protected def postGeneralQuery1(query: String): Future[JsValue] = {
    postGeneralQueryMysql(query)
  }
  protected def postGeneralQueryMysql(query: String): Future[JsValue] = {
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
          println("\r\n...."+columnLabel+"="+value.toString+",")
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
            case ts: TIMESTAMP =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(ts.dateValue().getTime)))
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

              Logger.warn(s" oracle type of value $value is not detectd")
              break
          }
        }
        qJsonArray = qJsonArray :+ rsJson
        System.out.println("qJsonArrayyyyyyyyyyy="+Json.toJson(qJsonArray).toString())
      }
    }
    Future(Json.toJson(qJsonArray))
  }

  protected def postGeneralQuery(query: String): Future[JsValue] = {
    val statement = connection.createStatement
    System.out.println("\r\n-----------postGeneralQuery="+query)
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
          val valueType = resultMetadata.getColumnTypeName(columnId)
          val value = result.getObject(columnLabel)
          println(columnLabel+"  value  + object"+value)

          valueType match {
            case "NUMBER" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(value.toString.toDouble))
            case "DATE" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(value.asInstanceOf[DATE].dateValue().getTime)))

            case "TIMESTAMP" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(value.asInstanceOf[TIMESTAMP].dateValue().getTime)))
            case "BLOB" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "LONG" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(value.toString.toLong))
            //            case "ROWID" =>
            //              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            //            case "TIMESTAMP WITH TIME ZONE" =>
            //              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(value.asInstanceOf[BigDecimal]))
            //            case "INTERVALDS" =>
            //              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(value.toInt))
            case "NCHAR" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "CHAR" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "VARCHAR2" =>
              //println("my result varchar2")
              if (value != null) {
                if (value.toString().contains("POINT(")) {

                  if (value.toString == "POINT(, )"){

                  }
                  else {
                    var coord = value.toString
                    var coordx = coord.slice(6, coord.length - 2).split(", ")(0)
                    var coordy = coord.slice(6, coord.length - 2).split(", ")(1)
                    val s = Seq(coordx.toDouble, coordy.toDouble)
                    rsJson = rsJson ++ Json.obj(columnLabel -> s)
                  }

                }
                else if (value.toString.contains("LINESTRING(")) {
                  var bound_boxStr = value.toString
                  var bound_boxFirstx = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(0).split(" ")(0).toDouble
                  var bound_boxFirsty = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(0).split(" ")(1).toDouble
                  var firstBound_box = Seq(bound_boxFirstx, bound_boxFirsty)
                  var bound_boxSecondx = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(1).split(" ")(0).toDouble
                  var bound_boxSecondy = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(1).split(" ")(1).toDouble
                  var secondBound_box = Seq(bound_boxSecondx, bound_boxSecondy)
                  var bound_box = Seq(firstBound_box, secondBound_box)
                  rsJson = rsJson ++ Json.obj(columnLabel -> bound_box)


                }
                else {
                  rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
                }
              }


            case "VARCHAR" =>
              //println("my result varchar")
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "NVARCHAR2" =>
              println("my result nvarchar2")
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "NCLOB" => //large data
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "CLOB" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case _ =>
              Logger.warn(s"type of value $value is not detectd")
              break
          }
          //rsJson = rsJson ++Json.obj(columnLabel -> JsNumber(value.toString().toInt))

          // System.out.println("rsJson="+rsJson)
        }
        qJsonArray = qJsonArray :+ rsJson
      }
    }
    System.out.println("qJsonArrayxxxxxxx="+Json.toJson(qJsonArray).toString())
    Future(Json.toJson(qJsonArray))
  }

  protected def postBerryQuery(query: String): Future[JsValue] = {

    val statement = connection.createStatement
    System.out.println("\r\n-----------postBerryQuery=="+ query)
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
    //Oracle sql can not end with ;
    System.out.println("\r\n-----------postControl="+query)
    val statement = connection.createStatement
    // println(query.split(";\n").mkString(" ")+"splitted")
    query.split("/\n").foreach {
      case q => println(q+" 1statement")
        statement.executeUpdate(q)
    }
    Future(true)



    //    val statement = connection.createStatement
    //    query.trim().split(";\n").foreach {
    //      case q => if (!q.isEmpty)   {
    //        if (q.endsWith(";")) {
    //          val sql = q.substring(0,q.length-1);
    //          statement.executeUpdate(sql);
    //        }
    //        else
    //          {
    //            statement.executeUpdate(q);
    //          }
    //      }
    //    }
    //    Future(true)
  }


  def postControlmysql(query: String) = {
    val statement = connection.createStatement
    query.split(";\n").foreach {
      case q => statement.executeUpdate(q)
    }
    Future(true)
  }

}

object OracleConn {
  val metaName = "berry.meta"
}
