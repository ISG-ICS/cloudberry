package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class PostgreSQLGenerator extends SQLGenerator {

  protected val quote = '"'
  //trunc(): truncate a number to a particular decimal places, mainly used in groupBy.
  protected val truncate: String = "trunc"
  protected val fullTextMatch: Seq[String] = Seq("@@", "to_tsquery")

  protected def parseCreate(create: CreateView, schemaMap: Map[String, AbstractSchema]): String = {
    val (temporalSchemaMap, lookupSchemaMap) = GeneratorUtil.splitSchemaMap(schemaMap)
    val sourceSchema = temporalSchemaMap(create.query.dataset)
    val resultSchema = calcResultSchema(create.query, sourceSchema)
    val ddl: String = genDDL(create.dataset, sourceSchema)
    val insert =
        s"""
       |insert into $quote${create.dataset}$quote
       |(
       |${parseQuery(create.query, schemaMap)}
       |) on conflict do nothing
        """.stripMargin
    ddl + insert
  }

  /**
    * Convert middleware datatype to PostgreSQL datatype
    * @param field
    */
  protected def fieldType2SQLType(field: Field): String = {
    field.dataType match {
      case DataType.Number => "bigint"
      case DataType.Time => "timestamp"
      case DataType.Point => "point"
      case DataType.Boolean => "smallint"
      case DataType.String => "varchar(255)"
      case DataType.Text => "text"
      case DataType.Bag => field.asInstanceOf[BagField].innerType match {
        case DataType.String => "varchar[]"
        case DataType.Number => "bigint[]"
      }
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
       |insert into $quote${q.dataset}$quote (${quote}name${quote}, ${quote}schema${quote}, ${quote}dataInterval${quote}, ${quote}stats${quote}, ${quote}stats.createTime${quote}) values
       |${queryResult.mkString(",")} on conflict(${quote}name${quote}) do update set ${quote}dataInterval${quote}=EXCLUDED.${quote}dataInterval${quote}
       |""".stripMargin
  }

  protected def initExprMap(dataset: String, schemaMap: Map[String, AbstractSchema]): Map[String, FieldExpr] = {
    val schema = schemaMap(dataset)
    schema.fieldMap.mapValues { f =>
      f match {
        case AllField =>
          FieldExpr(s"$sourceVar.$quote${f.name}$quote", s"$sourceVar.${f.name}")
        case _ =>
          FieldExpr(s"$sourceVar.$quote${f.name}$quote", s"$sourceVar.$quote${f.name}$quote")
      }
    }
  }

  protected def parseTextRelation(filter: FilterStatement, fieldExpr: String): String = {
    val wordsArr = ArrayBuffer[String]()
    filter.values.foreach(w => wordsArr += w.toString)
    val sb = new StringBuilder(s"$fieldExpr ${fullTextMatch(0)} ${fullTextMatch(1)}(")
    sb.append(wordsArr.mkString("'"," & ","')"))
    sb.toString()
  }

  protected def parseUnnest(unnest: Seq[UnnestStatement],
                  exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr] ++= exprMap
    val unnestTestStrs = new ListBuffer[String]
    unnest.zipWithIndex.map {
      case (unnest, id) =>
        val expr = exprMap(unnest.field.name)
        val newExpr = s"$unnestVar(${sourceVar}.${quote}${unnest.field.name}${quote})"
        producedExprs += (unnest.as.name -> FieldExpr(newExpr, newExpr))
        if (unnest.field.isOptional) {
          unnestTestStrs += s"${expr.refExpr} is not null"
        }
    }
    ParsedResult(unnestTestStrs, (producedExprs.result().toMap))
  }

  protected def parseGroupByFunc(groupBy: ByStatement, fieldExpr: String): String = {
    groupBy.funcOpt match {
      case Some(func) =>
        func match {
          case interval: Interval =>
            interval.unit match {
              case TimeUnit.Day => s"${timeUnitFuncMap(interval.unit)}($fieldExpr)"
              case _ => s"extract(${interval.unit} from $fieldExpr)"
            }
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
    * Process POINT type of PostgreSQL:
    * trunc(): truncate a number to a particular decimal places, mainly used in groupBy. http://w3resource.com/PostgreSQL/trunc-function.php
    * @param scale
    * @param fieldExpr
    * @param dataType
    */
  def parseGeoCell(scale: Integer, fieldExpr: String, dataType: DataType.Value): String = {
    s"($truncate($fieldExpr[0]::decimal,$scale),$truncate($fieldExpr[1]::decimal,$scale))"
  }

}

object PostgreSQLGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new PostgreSQLGenerator()
}
