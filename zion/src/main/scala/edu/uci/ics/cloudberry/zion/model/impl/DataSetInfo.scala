package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{Query, Schema}
import org.joda.time.{DateTime, Interval}

case class Stats(createTime: DateTime,
                 lastModifyTime: DateTime,
                 lastReadTime: DateTime,
                 cardinality: Long)

case class DataSetInfo(name: String,
                       createQueryOpt: Option[Query],
                       schema: Schema,
                       dataInterval: Interval,
                       stats: Stats)
