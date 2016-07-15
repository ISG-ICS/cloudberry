package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation

class Query(val dataset: String,
            val lookup: Seq[LookupStatement],
            val filter: Seq[FilterStatement],
            val groups: Option[GroupStatement],
            val select: Option[SelectStatement]
           ) extends Statement {

}

trait Statement {}

/**
  * Augments the source data to contain more fields.
  *
  * @param sourceKeys
  * @param lookupDataset
  * @param lookupKeys
  * @param selectValues
  * @param as
  */
class LookupStatement(val sourceKeys: Seq[String],
                      val lookupDataset: String,
                      val lookupKeys: Seq[String],
                      val selectValues: Seq[String],
                      val as: Seq[String]
                     ) extends Statement {
  //TODO to be replaced by a unified syntax exceptions
  require(sourceKeys.length == lookupKeys.length, "LookupStatement: lookup key number is different from size of the source key ")
  require(selectValues.length == as.length, "LookupStatement: select value names doesn't match with renamed names")
}

//TODO only support one transform for now
class FilterStatement(val fieldName: String,
                      val apply: Option[TransformFunc],
                      val relation: Relation = Relation.isTrue,
                      val values: Seq[AnyVal]
                     ) extends Statement {

}

/**
  * Groupby fieldNames
  *
  * @param fieldName
  * @param apply
  * @param groups //TODO support the auto group by given size
  */
class ByStatement(val fieldName: String,
                  val apply: Option[TransformFunc],
                  val as: Option[String]
                 ) extends Statement {

}

/**
  * The aggregate results produced by group by
  */
class AggregateStatement(val fieldName: String,
                         val apply: AggregateFunc,
                         val as: String
                        ) extends Statement {

}

class GroupStatement(val bys: Seq[ByStatement],
                     val aggregates: Seq[AggregateStatement]
                    ) extends Statement {

}

class SelectStatement(val order: Seq[String],
                      val limit: Int,
                      val offset: Int,
                      val fields: Seq[String]
                     ) extends Statement {

}


