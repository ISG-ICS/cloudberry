package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.Query
import org.joda.time.{DateTime, Interval}

case class Stats(createTime: DateTime,
                 lastModifyTime: DateTime,
                 lastReadTime: DateTime,
                 cardinality: Int)

case class DataSetInfo(name: String,
                       createQueryOpt: Option[Query],
                       dataInterval: Interval,
                       stats: Stats)
