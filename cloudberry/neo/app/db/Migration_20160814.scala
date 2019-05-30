package db

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.impl.{AsterixSQLPPConn, DataSetInfo, MySQLConn, OracleConn, PostgreSQLConn, ElasticsearchConn}
import scala.concurrent.{ExecutionContext, Future}
private[db] class Migration_20160814() {

  import Migration_20160814._

  //TODO it supposes to automatically register the dataset from AsterixDB
  def up(conn: IDataConn)(implicit ec: ExecutionContext): Future[Boolean] = {
    conn match {
      case psql: PostgreSQLConn =>
        conn.postControl{
          s"""
             |create table if not exists "berry.meta" (
             |"name" varchar(255) not null,
             |"schema" json not null,
             |"dataInterval" json not null,
             |"stats" json not null,
             |"stats.createTime" time not null,
             |primary key("name")
             |);
           """.stripMargin
        }
      case sql: MySQLConn =>
        conn.postControl {
          s"""
             |create table if not exists `berry.meta` (
             |`name` varchar(255) not null,
             |`schema` json not null,
             |`dataInterval` json not null,
             |`stats` json not null,
             |`stats.createTime` datetime not null,
             |primary key(`name`)
             |)
             |""".stripMargin
        }
      case oracle: OracleConn =>
          conn.postControl {
            s"""
               |declare
               |  result1 number(8);
               |begin
               |
               |  select count(*)into result1 from all_tables where owner = 'BERRY' and table_name = 'berry.meta';
               |
               |  if result1 = 0 then
               |    execute immediate '
               |    create table "berry.meta" (
               |      "name" varchar(255) not null,
               |      "schema" varchar2(4000) not null,
               |      "dataInterval" varchar2(255) not null,
               |      "stats" varchar (255) not null,
               |      "stats.createTime" timestamp not null,
               |      primary key ("name"))';
               |  end if;
               |end;
               |/\n""".stripMargin
          }
      case sqlpp: AsterixSQLPPConn =>
        conn.postControl {
          s"""
             |create dataverse berry if not exists;
             |create type berry.metaType if not exists as open {
             | name : string,
             | stats : { createTime: string}
             |};
             |
             |create dataset $berryMeta(berry.metaType) if not exists primary key name;
       """.stripMargin
        }
      case elasticsearch: ElasticsearchConn =>
        conn.postControl {
          s"""
             |{"mappings" : {
             |  "_doc" : {
             |    "properties" : {
             |      "dataInterval.start" : { "type" : "date", "format": "strict_date_time" },
             |      "dataInterval.end": { "type" : "date", "format": "strict_date_time" },
             |      "stats.createTime": { "type" : "date", "format": "strict_date_time" },
             |      "stats.lastModifyTime": { "type" : "date", "format": "strict_date_time" },
             |      "stats.lastReadTime": { "type" : "date", "format": "strict_date_time" }
             |    }
             |  }
             |},
             |"settings": {
             |  "index": {
             |    "max_result_window": 2147483647,
             |    "number_of_shards" : 1,
             |    "number_of_replicas" : 0
             |  }
             |},
             |"method": "create",
             |"dataset": "berry.meta"
             |}
           """.stripMargin
        }
    }
  }
}

object Migration_20160814 {
  val migration = new Migration_20160814()
  val berryMeta = DataSetInfo.MetaDataDBName
}
