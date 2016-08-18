package edu.uci.ics.cloudberry.zion.model.impl


import edu.uci.ics.cloudberry.zion.model.impl.TestDataSetInfo._
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
      val expectJSON = simpleDataSetInfoJSON
      val actualJSON = parser.write(simpleDataSetInfo)
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing Schema fields" in {
      val actualJSON = fieldsDataSetInfoJSON
      val expectJSON = parser.write(fieldsDataSetInfo)
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing a createQuery" in {
      ok
    }
    "read dataSetInfo containing complex fields" in {
      ok
    }
    "write dataSetInfo containing complex fields" in {
      ok
    }
  }
}
