package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{GeoCellThousandth, _}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json

object TestQuery {

  DateTimeZone.setDefault(DateTimeZone.UTC)
  val TwitterDataSet = TwitterDataStore.DatasetName
  val PopulationDataSet = PopulationDataStore.DatasetName
  val literacyDataSet = LiteracyDataStore.DatasetName
  val twitterSchema = TwitterDataStore.TwitterSchema
  val populationSchema = PopulationDataStore.PopulationSchema
  val literacySchema = LiteracyDataStore.LiteracySchema
  val startTime = "2016-01-01T00:00:00.000Z"
  val endTime = "2016-12-01T00:00:00.000Z"
  val twitterSchemaMap = Map(TwitterDataSet -> twitterSchema)
  val allSchemaMap = Map(TwitterDataSet -> twitterSchema, PopulationDataSet -> populationSchema, literacyDataSet -> literacySchema)


  val createAt = twitterField("create_at").asInstanceOf[TimeField]
  val hashtags = twitterField("hashtags")
  val text = twitterField("text")
  val tag = Field.as(hashtags, "tag")
  val geoStateID = twitterField("geo_tag.stateID")
  val isRetweet = twitterField("is_retweet")
  val id = twitterField("id")
  val geo = twitterField("geo")
  val coordinate = twitterField("coordinate")
  val userId = twitterField("user.id")
  val all = twitterField("*")

  val population = populationField("population")
  val stateID = populationField("stateID")

  val literacy = literacyField("literacy")

  val textValue = Seq("zika", "virus")
  val stateValue = Seq(37, 51, 24, 11, 10, 34, 42, 9, 44)
  val timeFilter = FilterStatement(createAt, None, Relation.inRange, Seq(startTime, endTime))
  val zikaFilter = FilterStatement(text, None, Relation.contains, Seq("zika"))
  val virusFilter = FilterStatement(text, None, Relation.contains, Seq("virus"))
  val textFilter = FilterStatement(text, None, Relation.contains, textValue)
  val stateFilter = FilterStatement(geoStateID, None, Relation.in, stateValue)
  val retweetFilter = FilterStatement(isRetweet, None, Relation.isTrue, Seq.empty)
  val bagFilter = FilterStatement(hashtags, None, Relation.contains, Seq(BagField("tags", DataType.String, false)))

  val intValues = Seq(1)
  val stringValue = Seq("English")
  val longValues: Seq[Long] = Seq(1644l, 45464l)
  val doubleValues: Seq[Double] = Seq(0.45541, 9.456)

  val sourceInterval = new org.joda.time.Interval(new DateTime(2015, 1, 1, 0, 0), new DateTime(2017, 1, 1, 0, 0))
  val sourceStat = Stats(sourceInterval.getStart, sourceInterval.getEnd, sourceInterval.getEnd, 10000)
  val sourceInfo = DataSetInfo(TwitterDataSet, None, twitterSchema, sourceInterval, sourceStat)

  val zikaHalfInterval = new org.joda.time.Interval(new DateTime(2015, 1, 1, 0, 0), new DateTime(2016, 6, 1, 0, 0))
  val zikaStats = Stats(zikaHalfInterval.getStart, zikaHalfInterval.getEnd, zikaHalfInterval.getEnd, 50)

  val zikaCreateQuery = Query(TwitterDataSet, filter = Seq(zikaFilter))
  val zikaHalfYearViewInfo = DataSetInfo("zika", Some(zikaCreateQuery), twitterSchema, zikaHalfInterval, zikaStats)

  val intFilter = FilterStatement(id, None, Relation.==, intValues)
  val stringFilter = FilterStatement(twitterField("lang"), None, Relation.matches, stringValue)
  val longFilter = FilterStatement(id, None, Relation.inRange, longValues)
  val doubleFilter = FilterStatement(id, None, Relation.inRange, doubleValues)


  val unnestHashTag = UnnestStatement(hashtags, tag)
  val byTag = ByStatement(tag, None, None)

  val secondInterval = Interval(TimeUnit.Second)
  val bySecond = ByStatement(createAt, Some(secondInterval), Some(Field.as(secondInterval(createAt), "sec")))
  val minuteInterval = Interval(TimeUnit.Minute)
  val byMinute = ByStatement(createAt, Some(Interval(TimeUnit.Minute)), Some(Field.as(minuteInterval(createAt), "min")))
  val hourInterval = Interval(TimeUnit.Hour)
  val byHour = ByStatement(createAt, Some(Interval(TimeUnit.Hour)), Some(Field.as(hourInterval(createAt), "hour")))
  val dayInterval = Interval(TimeUnit.Day)
  val byDay = ByStatement(createAt, Some(Interval(TimeUnit.Day)), Some(Field.as(dayInterval(createAt), "day")))
  val weekInterval = Interval(TimeUnit.Week)
  val byWeek = ByStatement(createAt, Some(Interval(TimeUnit.Week)), Some(Field.as(weekInterval(createAt), "week")))
  val monthInterval = Interval(TimeUnit.Month)
  val byMonth = ByStatement(createAt, Some(Interval(TimeUnit.Month)), Some(Field.as(monthInterval(createAt), "month")))
  val yearInterval = Interval(TimeUnit.Year)
  val byYear = ByStatement(createAt, Some(Interval(TimeUnit.Year)), Some(Field.as(minuteInterval(createAt), "year")))

  val level = Level("state")
  val byState = ByStatement(geo, Some(level), Some(Field.as(level(geo), "state")))
  val byGeocell10 = ByStatement(coordinate, Some(GeoCellTenth), Some(Field.as(GeoCellTenth(coordinate), "cell")))
  val byGeocell100 = ByStatement(coordinate, Some(GeoCellHundredth), Some(Field.as(GeoCellHundredth(coordinate), "cell")))
  val byGeocell1000 = ByStatement(coordinate, Some(GeoCellThousandth), Some(Field.as(GeoCellThousandth(coordinate), "cell")))
  val byUser = ByStatement(userId, None, None)

  val bin10 = Bin(10)
  val byBin = ByStatement(geoStateID, Some(bin10), Some(Field.as(bin10(geoStateID), "state")))

  val count = Field.as(Count(all), "count")
  val aggrCount = AggregateStatement(all, Count, count)
  val aggrMaxGroupBy = AggregateStatement(count, Max, Field.as(Max(count), "max"))
  val aggrMax = AggregateStatement(id, Max, Field.as(Max(id), "max"))
  val aggrMin = AggregateStatement(id, Min, Field.as(Min(id), "min"))
  val aggrSum = AggregateStatement(id, Sum, Field.as(Sum(id), "sum"))
  val aggrAvg = AggregateStatement(id, Avg, Field.as(Avg(id), "avg"))
  val aggrPopulationMin = AggregateStatement(population, Min, Field.as(Min(population), "min"))

  val groupPopulationSum = GroupStatement(
    bys = Seq(byState),
    aggregates = Seq(AggregateStatement(population, Sum, Field.as(Sum(population), "sum")))
  )

  val selectRecent = SelectStatement(Seq(createAt), Seq(SortOrder.DSC), 100, 0, Seq(createAt, id, userId))
  val selectTop10Tag = SelectStatement(Seq(count), Seq(SortOrder.DSC), 10, 0, Seq.empty)
  val selectTop10 = SelectStatement(Seq.empty, Seq(SortOrder.DSC), 10, 0, Seq.empty)

  val selectPopulation = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(all, population))
  val selectPopulationLiteracy = SelectStatement(Seq.empty, Seq.empty, 0, 0, Seq(all, population, literacy))

  val lookupPopulation = LookupStatement(
    sourceKeys = Seq(geoStateID),
    dataset = PopulationDataSet,
    lookupKeys = Seq(stateID),
    selectValues = Seq(population),
    as = Seq(Field.as(population, "population")))

  val lookupPopulationMultiple = LookupStatement(
    sourceKeys = Seq(geoStateID),
    dataset = PopulationDataSet,
    lookupKeys = Seq(stateID),
    selectValues = Seq(stateID, population),
    as = Seq(stateID, population))

  val lookupLiteracy = LookupStatement(
    sourceKeys = Seq(geoStateID),
    dataset = literacyDataSet,
    lookupKeys = Seq(stateID),
    selectValues = Seq(literacy),
    as = Seq(Field.as(literacy, "literacy")))

  val filterJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "geo_tag.stateID",
       |    "relation": "in",
       |    "values": [${stateValue.mkString(",")}]
       |  },
       |  {
       |    "field": "create_at",
       |    "relation": "inRange",
       |    "values": [
       |      "$startTime",
       |      "$endTime"
       |    ]
       |  },
       |  {
       |    "field": "text",
       |    "relation": "contains",
       |    "values": [
       |      ${textValue.map("\"" + _ + "\"").mkString(",")}
       |    ]
       |  }
       | ]
     """.stripMargin

  val filterZikaJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "text",
       |    "relation": "contains",
       |    "values": ["zika"]
       |  }
       | ]
     """.stripMargin

  val filterWrongValueJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "geo_tag.stateID",
       |    "relation": "in",
       |    "values": [[${stateValue.mkString(",")}]]
       |  }
       | ]
     """.stripMargin

  val filterWrongRelationJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "geo_tag.stateID",
       |    "relation": "iin",
       |    "values": [${stateValue.mkString(",")}]
       |  }
       | ]
     """.stripMargin

  val filterIntValueJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "id",
       |    "relation": "=",
       |    "values": [${intValues.mkString(",")}]
       |  }
       | ]
     """.stripMargin

  val filterStringValueJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "lang",
       |    "relation": "matches",
       |    "values": [${stringValue.map("\"" + _ + "\"").mkString(",")}]
       |  }
       | ]
     """.stripMargin

  val filterLongValueJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "id",
       |    "relation": "inRange",
       |    "values": [${longValues.mkString(",")}]
       |  }
       | ]
     """.stripMargin

  val filterDoubleValueJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "id",
       |    "relation": "inRange",
       |    "values": [${doubleValues.mkString(",")}]
       |  }
       | ]
     """.stripMargin


  val filterBooleanJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "is_retweet",
       |    "relation": "true",
       |    "values": []
       |  }
       | ]
     """.stripMargin

  val filterBagValueJSON =
    s"""
       |"filter": [
       |  {
       |    "field": "hashtag",
       |    "relation": "contains",
       |    "values": [${textValue.map("\"" + _ + "\"").mkString(",")}]
       |  }
       | ]
     """.stripMargin

  val filterSelectJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | $filterJSON,
       | "group": {
       |   "by": [
       |      {
       |        "field": "geo",
       |        "apply": {
       |          "name": "level",
       |          "args": {
       |            "level": "state"
       |          }
       |        },
       |        "as": "state"
       |      },
       |      {
       |        "field": "create_at",
       |        "apply": {
       |          "name": "interval",
       |          "args": {
       |            "unit": "hour"
       |          }
       |        },
       |        "as": "hour"
       |      }
       |    ],
       |   "aggregate": [
       |     {
       |       "field": "*",
       |       "apply": {
       |         "name": "count"
       |       },
       |       "as": "count"
       |     }
       |    ]
       |  }
       |}
    """.stripMargin
  )
  val groupByBinJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | "group": {
       |   "by": [
       |      {
       |        "field": "geo_tag.stateID",
       |        "apply": {
       |          "name": "bin",
       |          "args": {
       |            "scale": 10
       |          }
       |        },
       |        "as": "state"
       |      }
       |    ],
       |   "aggregate": [
       |     {
       |       "field": "*",
       |       "apply": {
       |         "name": "count"
       |       },
       |       "as": "count"
       |     }
       |    ]
       |  }
       |}
    """.stripMargin)

  val topKHashTagJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | $filterJSON,
       | "unnest" : [{ "hashtags": "tag"}],
       | "group": {
       |    "by": [
       |      {
       |        "field": "tag"
       |      }
       |    ],
       |    "aggregate": [
       |      {
       |        "field" : "*",
       |        "apply" : {
       |          "name": "count"
       |        },
       |        "as" : "count"
       |      }
       |    ]
       |  },
       |  "select" : {
       |    "order" : [ "-count"],
       |    "limit": 10,
       |    "offset" : 0
       |  }
       |}
     """.stripMargin)

  val sampleTweetJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterJSON,
       |   "select" : {
       |    "order" : [ "-create_at"],
       |    "limit": 100,
       |    "offset" : 0,
       |    "field": ["create_at", "id", "user.id"]
       |  }
       |}
       | """.stripMargin)


  val hourCountJSON = Json.parse(
    """
      |{
      |  "dataset": "twitter.ds_tweet",
      |  "group": {
      |    "by": [
      |      {
      |        "field": "create_at",
      |        "apply": {
      |          "name": "interval",
      |          "args" : {
      |            "unit": "hour"
      |          }
      |        },
      |        "as": "hour"
      |      }
      |    ],
      |    "aggregate": [
      |      {
      |        "field": "*",
      |        "apply": {
      |          "name" : "count"
      |        },
      |        "as": "count"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)

  val missingDatasetJSON = Json.parse(
    s"""
       |{
       |  $filterJSON,
       |   "select" : {
       |    "order" : [ "-create_at"],
       |    "limit": 100,
       |    "offset" : 0,
       |    "field": ["create_at", "id", "user.id"]
       |  }
       |}
       | """.stripMargin)

  val filterErrorJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterWrongValueJSON
       |}
       | """.stripMargin)

  val relationErrorJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterWrongRelationJSON
       |}
       | """.stripMargin)

  val intValuesJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterIntValueJSON
       |}
       | """.stripMargin)

  val stringValueJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterStringValueJSON
       |}
       | """.stripMargin)

  val longValuesJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterLongValueJSON
       |}
       | """.stripMargin)

  val doubleValuesJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterDoubleValueJSON
       |}
       | """.stripMargin)

  val bagValuesJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  $filterBagValueJSON
       |}
       | """.stripMargin)

  val geoCell10JSON = Json.parse(
    """
      |{
      |  "dataset": "twitter.ds_tweet",
      |  "group": {
      |    "by": [
      |      {
      |        "field": "coordinate",
      |        "apply": {
      |          "name": "geoCellTenth"
      |        },
      |        "as": "cell"
      |      }
      |    ],
      |    "aggregate": [
      |      {
      |        "field": "*",
      |        "apply": {
      |          "name" : "count"
      |        },
      |        "as": "count"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)

  val geoCell100JSON = Json.parse(
    """
      |{
      |  "dataset": "twitter.ds_tweet",
      |  "group": {
      |    "by": [
      |      {
      |        "field": "coordinate",
      |        "apply": {
      |          "name": "geoCellHundredth"
      |        },
      |        "as": "cell"
      |      }
      |    ],
      |    "aggregate": [
      |      {
      |        "field": "*",
      |        "apply": {
      |          "name" : "count"
      |        },
      |        "as": "count"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)

  val geoCell1000JSON = Json.parse(
    """
      |{
      |  "dataset": "twitter.ds_tweet",
      |  "group": {
      |    "by": [
      |      {
      |        "field": "coordinate",
      |        "apply": {
      |          "name": "geoCellThousandth"
      |        },
      |        "as": "cell"
      |      }
      |    ],
      |    "aggregate": [
      |      {
      |        "field": "*",
      |        "apply": {
      |          "name" : "count"
      |        },
      |        "as": "count"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin)

  val booleanFilterJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | $filterBooleanJSON,
       | "group": {
       |    "by": [
       |      {
       |        "field": "user.id"
       |      }
       |    ],
       |    "aggregate": [
       |      {
       |        "field" : "*",
       |        "apply" : {
       |          "name": "count"
       |        },
       |        "as" : "count"
       |      }
       |    ]
       |  }
       |}
     """.stripMargin)

  val globalCountJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | "global": {
       |   "globalAggregate":
       |     {
       |       "field": "*",
       |       "apply": {
       |         "name": "count"
       |       },
       |       "as": "count"
       |     }
       |  }
       |}
    """.stripMargin)

  val globalMaxJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | "global": {
       |   "globalAggregate":
       |     {
       |       "field": "id",
       |       "apply": {
       |         "name": "max"
       |       },
       |       "as": "max"
       |     }
       |  }
       |}
    """.stripMargin)

  val globalMinJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | "global": {
       |   "globalAggregate":
       |     {
       |       "field": "id",
       |       "apply": {
       |         "name": "min"
       |       },
       |       "as": "min"
       |     }
       |  }
       |}
    """.stripMargin)

  val zikaJSON = Json.parse(
    s"""
       |{
       | "dataset": "twitter.ds_tweet",
       | $filterZikaJSON
       |}
    """.stripMargin)

  val simpleLookupFilterJSON = Json.parse(
    s"""
       |{
       | "dataset":"twitter.ds_tweet",
       | "lookup": [
       |    {
       |      "joinKey":["geo_tag.stateID"],
       |      "dataset":"twitter.US_population",
       |      "lookupKey":["stateID"],
       |      "select":["population"],
       |      "as" : ["population"]
       |    }
       |   ],
       | "filter":[
       |    {
       |      "field":"text",
       |      "relation":"contains",
       |      "values":[${textValue.map("\"" + _ + "\"").mkString(",")}]
       |    }
       |  ],
       |  "select": {
       |    "order" : [],
       |    "limit" : 0,
       |    "offset" : 0,
       |    "field" : ["*","population"]
       |  }
       |}
    """.stripMargin)

  val multiFieldLookupFilterJSON = Json.parse(
    s"""
       |{
       | "dataset":"twitter.ds_tweet",
       | "lookup": [
       |    {
       |      "joinKey":["geo_tag.stateID"],
       |      "dataset":"twitter.US_population",
       |      "lookupKey":["stateID"],
       |      "select":["stateID", "population"],
       |      "as" : ["stateID", "population"]
       |    }
       |   ],
       | "filter":[
       |    {
       |      "field":"text",
       |      "relation":"contains",
       |      "values":[${textValue.map("\"" + _ + "\"").mkString(",")}]
       |    }
       |  ],
       |  "select": {
       |    "order" : [],
       |    "limit" : 0,
       |    "offset" : 0,
       |    "field" : ["*","population"]
       |  }
       |}
    """.stripMargin)

  val multiLookupFilterJSON = Json.parse(
    s"""
       |{
       | "dataset":"twitter.ds_tweet",
       | "lookup": [
       |    {
       |      "joinKey":["geo_tag.stateID"],
       |      "dataset":"twitter.US_population",
       |      "lookupKey":["stateID"],
       |      "select":["population"],
       |      "as" : ["population"]
       |    },
       |    {
       |      "joinKey":["geo_tag.stateID"],
       |      "dataset":"twitter.US_literacy",
       |      "lookupKey":["stateID"],
       |      "select":["literacy"],
       |      "as" : ["literacy"]
       |    }
       |   ],
       | "filter":[
       |    {
       |      "field":"text",
       |      "relation":"contains",
       |      "values":[${textValue.map("\"" + _ + "\"").mkString(",")}]
       |    }
       |  ],
       |  "select": {
       |    "order" : [],
       |    "limit" : 0,
       |    "offset" : 0,
       |    "field" : ["*","population", "literacy"]
       |  }
       |}
     """.stripMargin
  )

  def removeEmptyLine(string: String): String = string.split("\\r?\\n").filterNot(_.trim.isEmpty).mkString("\n")

  def unifyNewLine(string: String): String = string.replaceAll("\\r?\\n", "\n")

  def twitterField(field: String): Field = twitterSchema.fieldMap(field)

  def populationField(field: String): Field = populationSchema.fieldMap(field)

  def literacyField(field: String): Field = literacySchema.fieldMap(field)

}
