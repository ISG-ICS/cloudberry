package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.model.{IdSetPredicate, KeywordPredicate, TimePredicate}
import org.joda.time.{DateTime, Interval, Weeks}
import org.specs2.mutable.Specification

class AQLVisitorTest extends Specification {

  import TwitterDataStoreActor._

  val aqlVisitor = AQLVisitor(Name)

  val startTime1 = new DateTime(2012, 1, 1, 0, 0)
  val interval1 = new Interval(startTime1, Weeks.weeks(1))

  val startTime2 = new DateTime(2016, 1, 1, 0, 0)
  val interval2 = new Interval(startTime2, Weeks.weeks(2))

  "AQLVisitorTest" should {
    "visit one time predicate" in {
      val timePredicate1 = TimePredicate(FieldCreateAt, Seq[Interval](interval1))
      val str1 = aqlVisitor.visitPredicate("t", timePredicate1)
      str1.trim() must beEqualTo(
        s"""
           |where
           |
           |($$t."create_at">= datetime("2012-01-01T00:00:00.000Z")
           |and $$t."create_at" < datetime("2012-01-08T00:00:00.000Z"))
         """.stripMargin.trim())
    }
    "visit two time predicate" in {
      val timePredicate2 = TimePredicate(FieldCreateAt, Seq[Interval](interval1, interval2))
      val str2 = aqlVisitor.visitPredicate("t", timePredicate2)
      str2.trim must beEqualTo(
        """
          |where
          |
          |($t."create_at">= datetime("2012-01-01T00:00:00.000Z")
          |and $t."create_at" < datetime("2012-01-08T00:00:00.000Z"))
          |            or
          |($t."create_at">= datetime("2016-01-01T00:00:00.000Z")
          |and $t."create_at" < datetime("2016-01-15T00:00:00.000Z"))
          |
        """.stripMargin.trim)
    }
    "visit id list predicate" in {
      val idPredicate = IdSetPredicate(FieldStateID, Seq(1, 2, 3))
      val str = aqlVisitor.visitPredicate("t", idPredicate)
      str.trim must beEqualTo(
        """
          |let $set := [ 1,2,3 ]
          |for $sid in $set
          |where $t."geo_tag.stateID" = $sid
        """.stripMargin.trim
      )
    }
    "visit one keyword predicate" in {
      val keywordPredicate = KeywordPredicate(FieldKeyword, Seq("trump"))
      val str = aqlVisitor.visitPredicate("t", keywordPredicate)
      str.trim must beEqualTo(
        """
          |where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0
        """.stripMargin.trim
      )
    }
    "visit two keyword predicate" in {
      val keywordPredicate = KeywordPredicate(FieldKeyword, Seq("trump", "hilary"))
      val str = aqlVisitor.visitPredicate("t", keywordPredicate)
      str.trim must beEqualTo(
        """where similarity-jaccard(word-tokens($t."text"), word-tokens("trump")) > 0.0 """ + "\n" +
        """where similarity-jaccard(word-tokens($t."text"), word-tokens("hilary")) > 0.0"""
      )
    }
  }
}
