package edu.uci.ics.cloudberry.zion.model.impl


import edu.uci.ics.cloudberry.zion.model.impl.TestDataSetInfo._
import org.specs2.mutable.Specification

class DataSetInfoTest extends Specification {

  val parser = DataSetInfo

  import TestQuery.twitterSchemaMap

  "DataSetInfo" should {
    "read a sample dataSetInfo" in {
      val actualDataSetInfoy = parser.parse(simpleDataSetInfoJSON, Map.empty)
      val expectDataSetInfo = simpleDataSetInfo
      actualDataSetInfoy must_== expectDataSetInfo
    }
    "read dataSetInfo containing Schema fields" in {
      val actualDataSetInfoy = parser.parse(fieldsDataSetInfoJSON, Map.empty)
      val expectDataSetInfo = fieldsDataSetInfo
      actualDataSetInfoy must_== expectDataSetInfo
    }
    "read dataSetInfo containing a createQuery" in {
      val actualDataSetInfoy = parser.parse(queryDataSetInfoJSON, twitterSchemaMap)
      val expectDataSetInfo = queryDataSetInfo
      actualDataSetInfoy must_== expectDataSetInfo
    }
    "read dataSetInfo with createQuery by (state, hour) count request" in {
      val actualDataSetInfo = parser.parse(complexQueryDataSetInfoJSON, twitterSchemaMap)
      val expectDataSetInfo = berryAggrByTagViewDataSetInfo
      actualDataSetInfo must_== expectDataSetInfo
    }
    "read dataSetInfo containing dimension and measurement fields" in {
      val actualDataSetInfo = parser.parse(sourceDataSetInfoJSON, twitterSchemaMap)
      val expectDataSetInfo = TestQuery.sourceInfo
      actualDataSetInfo must_== expectDataSetInfo
    }
    "read dataSetInfo containing twitter schema and zika filter query" in {
      val actualDataSetInfo = parser.parse(zikaDataSetInfoJSON, twitterSchemaMap)
      val expectDataSetInfo = TestQuery.zikaHalfYearViewInfo
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
      val actualJSON = parser.write(berryAggrByTagViewDataSetInfo)
      val expectJSON = complexQueryDataSetInfoJSON
      actualJSON must_== expectJSON
    }

    "write dataSetInfo with createQuery by topK hashtag request" in {
      val actualJSON = parser.write(unnestQueryDataSetInfo)
      val expectJSON = unnestQueryDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing dimension and measurement fields" in {
      val actualJSON = parser.write(TestQuery.sourceInfo)
      val expectJSON = sourceDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing twitter schema and zika filter query" in {
      val actualJSON = parser.write(TestQuery.zikaHalfYearViewInfo)
      val expectJSON = zikaDataSetInfoJSON
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing twitter schema and group by bin query" in {
      val actualJSON = parser.write(byBinDataSetInfo)
      val expectJSON = BinDataSetInfoJSON
      actualJSON must_== expectJSON
    }
  }
}
