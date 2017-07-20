package edu.uci.ics.cloudberry.zion.actor

import akka.actor.Props
import akka.pattern.ask
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

class MetaDataAgent(override val dbName: String,
                    override val schema: Schema,
                    override val queryParser: IQLGenerator,
                    override val conn: IDataConn,
                    override val config: Config
                   )(implicit ec: ExecutionContext)
  extends AbstractUpdatableDataSetAgent(dbName, schema, queryParser, conn, config)(ec) {

  override protected def estimate(query: Query): Option[JsValue] = None

  override protected def maintenanceWork: Receive = {
    case upsert: UpsertRecord =>
      processUpdate(queryParser.generate(upsert, Map(dbName -> schema)))
    case delete: DeleteRecord =>
      processUpdate(queryParser.generate(delete, Map(dbName -> schema)))
    case drop: DropView =>
      // Note: parameter SchemaMap is not used in this case.
      val dropStatement = queryParser.generate(drop, Map())
      val viewRecordFilter = FilterStatement(DataSetInfo.MetaSchema.fieldMap("name"), None, Relation.matches, Seq(drop.dataset))
      val delete = DeleteRecord(DataSetInfo.MetaDataDBName, Seq(viewRecordFilter))
      val deleteStatement = queryParser.generate(delete, Map(dbName -> schema))
      processUpdate(Seq(dropStatement, deleteStatement))
  }
}

object MetaDataAgent {
  def props(dbName: String, schema: Schema, queryParser: IQLGenerator, conn: IDataConn, config: Config)
           (implicit ec: ExecutionContext) =
    Props(new MetaDataAgent(dbName, schema, queryParser, conn, config))
}
