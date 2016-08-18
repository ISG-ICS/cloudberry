package edu.uci.ics.cloudberry.zion.model.impl


import edu.uci.ics.cloudberry.zion.model.impl.TestDataSetInfo._
import edu.uci.ics.cloudberry.zion.model.schema.{GroupStatement, Query}
import org.specs2.mutable.Specification

class DataSetInfoTest extends Specification {

  val parser = DataSetInfo

  "DataSetInfo" should {
    "read a sample dataSetInfo" in {
      val actualQuery = parser.parse(simpleDataSetInfoJSON)
      val expectQuery = simpleDataSetInfo
      actualQuery must_== expectQuery
    }
    "read dataSetInfo containing Schema fields" in {
      val actualQuery = parser.parse(fieldsDataSetInfoJSON)
      val expectQuery = fieldsDataSetInfo
      actualQuery must_== expectQuery
    }
    "read dataSetInfo containing a createQuery" in {
      val actualQuery = parser.parse(queryDataSetInfoJSON)
      val expectQuery = queryDataSetInfo
      actualQuery must_== expectQuery
    }
    "write a sample dataSetInfo" in {
      val expectJSON = parser.write(simpleDataSetInfo)
      val actualJSON = simpleDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing Schema fields" in {
      val actualJSON = parser.write(fieldsDataSetInfo)
      val expectJSON = fieldsDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing a createQuery" in {
      val actualJSON = parser.write(queryDataSetInfo)
      val expectJSON = queryDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "read dataSetInfo containing complex fields" in {
      ok
    }
    "write dataSetInfo containing complex fields" in {
      ok
    }
  }
}
