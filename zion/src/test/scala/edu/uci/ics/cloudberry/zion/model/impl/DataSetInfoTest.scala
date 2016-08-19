package edu.uci.ics.cloudberry.zion.model.impl


import edu.uci.ics.cloudberry.zion.model.impl.TestDataSetInfo._
import org.specs2.mutable.Specification

class DataSetInfoTest extends Specification {

  val parser = DataSetInfo

  "DataSetInfo" should {
    "read a sample dataSetInfo" in {
      val actualDataSetInfoy = parser.parse(simpleDataSetInfoJSON)
      val expectDataSetInfo = simpleDataSetInfo
      actualDataSetInfoy must_== expectDataSetInfo
    }
    "read dataSetInfo containing Schema fields" in {
      val actualDataSetInfoy = parser.parse(fieldsDataSetInfoJSON)
      val expectDataSetInfo = fieldsDataSetInfo
      actualDataSetInfoy must_== expectDataSetInfo
    }
    "read dataSetInfo containing a createQuery" in {
      val actualDataSetInfoy = parser.parse(queryDataSetInfoJSON)
      val expectDataSetInfo = queryDataSetInfo
      actualDataSetInfoy must_== expectDataSetInfo
    }
    "read dataSetInfo with createQuery by (state, hour) count request" in {
      val actualDataSetInfo = parser.parse(complexQueryDataSetInfoJSON)
      val expectDataSetInfo = complexQueryDataSetInfo
      actualDataSetInfo must_== expectDataSetInfo
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
    "write dataSetInfo containing a simple createQuery" in {
      val actualJSON = parser.write(queryDataSetInfo)
      val expectJSON = queryDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "write dataSetInfo with createQuery by (state, hour) count request" in {
      val actualJSON = parser.write(complexQueryDataSetInfo)
      val expectJSON = complexQueryDataSetInfoJSON
      actualJSON must_== expectJSON
    }

    "write dataSetInfo with createQuery by topK hashtag request" in {
      val actualJSON = parser.write(unnestQueryDataSetInfo)
      val expectJSON = unnestQueryDataSetInfoJSON
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
