package migration

import models.AQLConnection
import play.api.Logger
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class Migration_20160324(val connection: AQLConnection) {

  import Migration_20160324._

  def up(): Future[WSResponse] = {
    Logger.logger.info("Migration create table")
    createTable()
  }

  def down(): Future[WSResponse] = {
    dropTable()
  }

  private def createTable() = {
    connection.post(
      s"""
         |use dataverse $Dataverse
         |
         |create type type$ViewMetaDataset if not exists as open {
         | "dataset": string,
         | "keyword": string,
         | "timeStart": datetime,
         | "timeEnd": datetime
         |}
         |
         |create dataset $ViewMetaDataset(type$ViewMetaDataset) if not exists primary key "dataset","keyword";
         |
      """.stripMargin)
  }

  private def dropTable() = {
    connection.post(
      s"""
         |use dataverse $Dataverse
         |
         |drop dataset $ViewMetaDataset if exists;
      """.stripMargin)

  }
}

object Migration_20160324 {
  val Dataverse = "twitter"
  val ViewMetaDataset = "viewMeta"

}
