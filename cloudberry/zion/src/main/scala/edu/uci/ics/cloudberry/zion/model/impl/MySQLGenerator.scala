package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class MySQLGenerator extends SQLGenerator {

  protected val quote = '`'
  protected val truncate: String = "truncate"
  protected val fullTextMatch = Seq("match", "against")

  //converts a value in internal geometry format to its plain text representation, e.g.: "POINT(1, 2)"
  private val geoAsText: String = "st_astext"
  //X/Y-coordinate value for the Point object in MySQL.
  private val pointGetCoord = Seq("st_x", "st_y")

  protected def parseCreate(create: CreateView, schemaMap: Map[String, AbstractSchema]): String = {
    val (temporalSchemaMap, lookupSchemaMap) = GeneratorUtil.splitSchemaMap(schemaMap)
    val sourceSchema = temporalSchemaMap(create.query.dataset)
    val resultSchema = calcResultSchema(create.query, sourceSchema)
    val ddl: String = genDDL(create.dataset, sourceSchema)
    val insert =
      s"""
     |replace into $quote${create.dataset}$quote
     |(
     |${parseQuery(create.query, schemaMap)}
     |)
      """.stripMargin
    ddl + insert
  }

  /**
    * Convert middleware datatype to MySQL datatype
    * @param field
    */
  protected def fieldType2SQLType(field: Field): String = {
    field.dataType match {
      case DataType.Number => "bigint"
      case DataType.Time => "datetime"
      case DataType.Point => "point"
      case DataType.Boolean => "tinyint"
      case DataType.String => "varchar(255)"
      case DataType.Text => "text"
      case DataType.Bag => ???
      case DataType.Hierarchy => ???
      case DataType.Record => ???
    }
  }

  protected def parseUpsertMeta(q: UpsertRecord): String = {
    val records = q.records.value
    var queryResult = ArrayBuffer.empty[String]
    records.foreach {
      record =>
        val name: String = (record \ "name").as[JsString].value
        val schema: JsValue = (record \ "schema").as[JsValue]
        val dataInterval: JsValue = (record \ "dataInterval").as[JsValue]
        val stats: JsValue = (record \ "stats").as[JsValue]
        val createTime: String = TimeField.TimeFormatForSQL.print(new DateTime((record \ "stats" \ "createTime").as[String]))
        queryResult += (s"('${name}','${schema}','${dataInterval}','${stats}','${createTime}')")
    }
    s"""
       |replace into $quote${q.dataset}$quote (${quote}name${quote}, ${quote}schema${quote}, ${quote}dataInterval${quote}, ${quote}stats${quote}, ${quote}stats.createTime${quote}) values
       |${queryResult.mkString(",")}
       |""".stripMargin
  }

  protected def initExprMap(dataset: String, schemaMap: Map[String, AbstractSchema]): Map[String, FieldExpr] = {
    val schema = schemaMap(dataset)
    schema.fieldMap.mapValues {
      f => FieldExpr(s"$sourceVar.$quote${f.name}$quote", s"$sourceVar.$quote${f.name}$quote")
    }
  }

  protected def parseTextRelation(filter: FilterStatement, fieldExpr: String): String = {
    val wordsArr = ArrayBuffer[String]()
    filter.values.foreach(w => wordsArr += w.toString)
    val sb = new StringBuilder(s"${fullTextMatch(0)}($fieldExpr) ${fullTextMatch(1)} ('")
    sb.append(wordsArr.mkString("+"," +","") + s"' in boolean mode)")
    sb.toString()
  }

  //TODO: unnest
  protected def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    //return the empty result & exprMap for next step's process.
    ParsedResult((new ListBuffer[String]), exprMap)
  }

  protected def parseGroupByFunc(groupBy: ByStatement, fieldExpr: String): String = {
    groupBy.funcOpt match {
      case Some(func) =>
        func match {
          case interval: Interval => s"${timeUnitFuncMap(interval.unit)}($fieldExpr)"
          case GeoCellTenth => parseGeoCell(1, fieldExpr, groupBy.field.dataType)
          case GeoCellHundredth => parseGeoCell(2, fieldExpr, groupBy.field.dataType)
          case GeoCellThousandth => parseGeoCell(3, fieldExpr, groupBy.field.dataType)
          case bin: Bin => s"$round($fieldExpr/${bin.scale})*${bin.scale}"
          case _ => throw new QueryParsingException(s"unknown function: ${func.name}")
        }
      case None => fieldExpr
    }
  }


  /**
    * Process POINT type of MySQL:
    * ST_ASTEXT: return POINT field as text to avoid messy code. https://dev.mysql.com/doc/refman/5.7/en/gis-format-conversion-functions.html
    * ST_X, ST_Y: get X/Y-coordinate of Point. https://dev.mysql.com/doc/refman/5.6/en/gis-point-property-functions.html
    * truncate: a number truncated to a certain number of decimal places, mainly used in groupBy. http://www.w3resource.com/mysql/mathematical-functions/mysql-truncate-function.php
    * @param scale
    * @param fieldExpr
    * @param dataType
    */
  def parseGeoCell(scale: Integer, fieldExpr: String, dataType: DataType.Value): String = {
    s"$geoAsText($dataType($truncate(${pointGetCoord(0)}($fieldExpr),$scale),$truncate(${pointGetCoord(1)}($fieldExpr),$scale))) "
  }

}

object MySQLGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new MySQLGenerator()
}
