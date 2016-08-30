package db

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats, TwitterDataStore}
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

private[db] class Migration_20160814() {

  import Migration_20160814._

  //TODO it supposes to automatically register the dataset from AsterixDB
  def up(conn: IDataConn)(implicit ec: ExecutionContext): Future[Boolean] = {
    //TODO generate the schema and fetch the stats automatically
    val interval = new Interval(new DateTime(2016, 6, 30, 0, 0), DateTime.now())
    val stats = Stats(new DateTime(2016, 6, 30, 0, 0), DateTime.now, DateTime.now, 100 * 1000 * 1000)
    val twitterInfo = DataSetInfo(TwitterDataStore.DatasetName, None, TwitterDataStore.TwitterSchema, interval, stats)
    conn.postControl {
      s"""
         |create dataverse berry if not exists;
         |create type berry.metaType if not exists as open {
         | name : string,
         | stats : { createTime: string}
         |}
         |
         |create dataset $berryMeta(berry.metaType) if not exists primary key name;
         |
         |upsert into dataset $berryMeta (
         |  ${Json.toJson(twitterInfo)}
         |)
       """.stripMargin
    }
  }
}

object Migration_20160814 {
  val migration = new Migration_20160814()
  val berryMeta = "berry.meta"
}
