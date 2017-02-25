package edu.uci.ics.cloudberry.zion.actor

import akka.actor.Props
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query, Schema}
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

class ViewDataAgent(override val dbName: String,
                    override val schema: Schema,
                    override val queryParser: IQLGenerator,
                    override val conn: IDataConn,
                    override val config: Config
                   )(implicit ec: ExecutionContext)
  extends AbstractUpdatableDataSetAgent(dbName, schema, queryParser, conn, config)(ec) {

  //TODO to speed up the count performance when updating views
  override protected def estimate(query: Query): Option[JsValue] = None

  override protected def maintenanceWork: Receive = {
    case append: AppendView =>
      processUpdate(queryParser.generate(append, schema))
  }
}

object ViewDataAgent {
  def props(dbName: String, schema: Schema, queryParser: IQLGenerator, conn: IDataConn, config: Config)
           (implicit ec: ExecutionContext) =
    Props(new ViewDataAgent(dbName, schema, queryParser, conn, config))
}

