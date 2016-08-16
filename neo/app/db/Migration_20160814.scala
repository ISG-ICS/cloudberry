package db

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats, TwitterDataStore}
import org.joda.time.{DateTime, Interval}

import scala.concurrent.{ExecutionContext, Future}

private[db] class Migration_20160814() {

  //TODO add the meta data read logic
  def up(conn: IDataConn)(implicit ec: ExecutionContext): Future[Seq[DataSetInfo]] = {
    Future {
      val interval = new Interval(0, DateTime.now().getMillis)
      val stats = Stats(new DateTime(0), DateTime.now, DateTime.now, 60 * 1000 * 1000)
      Seq(DataSetInfo(TwitterDataStore.DatasetName, None, TwitterDataStore.TwitterSchema, interval, stats))
    }
  }
}

object Migration_20160814 {
  val migration = new Migration_20160814()
}
