package db

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, SQLConn, SQLGenerator, SparkConn}

import scala.concurrent.{ExecutionContext, Future}

private[db] class Migration_20160814() {

  import Migration_20160814._

  //TODO it supposes to automatically register the dataset from AsterixDB
  def up(conn: IDataConn)(implicit ec: ExecutionContext): Future[Boolean] = {
    conn match {
      case sql: SQLConn =>
        conn.postControl {
          s"""
             |CREATE table if not exists `berry.meta` (
             |name varchar(255) default null,
             |`schema` text default null,
             |create_at datetime default null
             |)
             |""".stripMargin
        }
      case _ =>
        conn.postControl {
          s"""
             |create dataverse berry if not exists;
             |create type berry.metaType if not exists as open {
             | name : string,
             | stats : { createTime: string}
             |}
             |
             |create dataset $berryMeta(berry.metaType) if not exists primary key name;
       """.stripMargin
        }
    }
  }
}

object Migration_20160814 {
  val migration = new Migration_20160814()
  val berryMeta = DataSetInfo.MetaDataDBName
}
