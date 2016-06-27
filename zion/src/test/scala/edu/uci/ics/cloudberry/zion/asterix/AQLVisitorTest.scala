package edu.uci.ics.cloudberry.zion.asterix

import org.specs2.mutable.Specification

class AQLVisitorTest extends Specification with TestData {

  import TwitterDataStoreActor._

  val aqlVisitor = AQLVisitor(Name)


  "AQLVisitorTest" should {
    "visit one time predicate" in {
      val str1 = aqlVisitor.visitPredicate("t", timePredicate1)
      str1.trim() must beEqualTo(
        s"""
           |where
           |
           |($$t."create_at">= datetime("2012-01-01T00:00:00.000Z")
           |and $$t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
           |""".stripMargin.trim())
    }
    "visit two time predicate" in {
      val str2 = aqlVisitor.visitPredicate("t", timePredicate2)
      str2.trim must beEqualTo(
        """
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2012-01-08T00:00:00.000Z"))
          |or
          |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
          |and $t."create_at" <= datetime("2016-01-15T00:00:00.000Z"))
          |""".stripMargin.trim)
    }
    "visit id list predicate" in {
      val str = aqlVisitor.visitPredicate("t", idPredicate)
      str.trim must beEqualTo(
        """
          |for $sid in [ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ]
          |where $t.geo_tag.stateID = $sid
          |""".stripMargin.trim
      )
    }
    "visit one keyword predicate" in {
      val str = aqlVisitor.visitPredicate("t", keywordPredicate1)
      str.trim must beEqualTo(
        """
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
          |""".stripMargin.trim
      )
    }
    "visit two keyword predicate" in {
      val str = aqlVisitor.visitPredicate("t", keywordPredicate2)
      str.trim must beEqualTo(
        """
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
          |and contains($t."text", "hilary")
          |""".stripMargin.trim
      )
    }
  }
}
