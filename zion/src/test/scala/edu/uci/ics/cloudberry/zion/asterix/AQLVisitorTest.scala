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
           |and $$t."create_at" < datetime("2012-01-08T00:00:00.000Z"))
           |""".stripMargin.trim())
    }
    "visit two time predicate" in {
      val str2 = aqlVisitor.visitPredicate("t", timePredicate2)
      str2.trim must beEqualTo(
        """
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" < datetime("2012-01-08T00:00:00.000Z"))
          |or
          |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
          |and $t."create_at" < datetime("2016-01-15T00:00:00.000Z"))
          |""".stripMargin.trim)
    }
    "visit id list predicate" in {
      val str = aqlVisitor.visitPredicate("t", idPredicate)
      str.trim must beEqualTo(
        """
          |let $set := [ 1,2,3 ]
          |for $sid in $set
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
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("hilary")) > 0.0
          |""".stripMargin.trim
      )
    }
  }
}
