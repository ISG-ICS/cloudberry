package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{AbstractSchema, LookupSchema, Schema}


object GeneratorUtil {
  def splitSchemaMap(schemaMap: Map[String, AbstractSchema]): (Map[String, Schema], Map[String, LookupSchema]) = {
    val temporalSchemaMap = scala.collection.mutable.Map[String, Schema]()
    val lookupSchemaMap = scala.collection.mutable.Map[String, LookupSchema]()

    schemaMap.filter{ case(name, schema) =>
      schema.isInstanceOf[Schema]
    }.foreach{ case(name, schema) =>
      temporalSchemaMap.put(name, schema.asInstanceOf[Schema])
    }

    schemaMap.filter{ case(name, schema) =>
      schema.isInstanceOf[LookupSchema]
    }.foreach{ case(name, schema) =>
      lookupSchemaMap.put(name, schema.asInstanceOf[LookupSchema])
    }

    (temporalSchemaMap.toMap, lookupSchemaMap.toMap)
  }
}