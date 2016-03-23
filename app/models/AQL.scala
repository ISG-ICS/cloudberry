package models

import actors.ParsedQuery
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat

class AQL(val statement: String) {

}

object AQL {
  val Dataverse = "twitter"
  val ViewMetaDataset = "viewMeta"
  val TweetsType = "type_tweet"
  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  def toKeywordSelection(query: Any): AQL = ???

  def toTimeIntervalSelection(query: Any): AQL = ???

  def toSpatialSelection(query: Any): AQL = ???

  def updateView(fromDataset: DataSet, viewName: String, keyword: String, interval: Interval): AQL = {
    new AQL(
      s"""
         |use dataverse $Dataverse
         |create dataset $viewName($TweetsType) if not exists primary key id;
         |insert into dataset $viewName(
         |let $$ts_start := datetime("${TimeFormat.print(interval.getStart)}")
         |let $$ts_end := datetime("${TimeFormat.print(interval.getEnd)}")
         |for $$t in dataset ${fromDataset.name}
         |let $$keyword0 := "$keyword"
         |where $$t.place.place_type = "city"
         |and $$t.create_at >= $$ts_start and $$t.create_at < $$ts_end
         |and contains($$t.text_msg, $$keyword0)
         |return {
         |  "create_at" : $$t.create_at,
         |  "id": $$t.id,
         |  "text_msg" : $$t.text_msg,
         |  "in_reply_to_status" : $$t.in_reply_to_status,
         |  "in_reply_to_user" : $$t.in_reply_to_user,
         |  "favorite_count" : $$t.favorite_count,
         |  "geo_location": $$t.geo_location,
         |  "retweet_count" : $$t.retweet_count,
         |  "lang" : $$t.lang,
         |  "is_retweet": $$t.is_retweet,
         |  "hashtags" :$$t.hashtags,
         |  "user_mentions" : $$t.user_mentions ,
         |  "user" : $$t.user,
         |  "place" : $$t.place,
         |  "state" : substring-after($$t.place.full_name, ", "),
         |  "city" : substring-before($$t.place.full_name, ","),
         |  "county": (for $$city in dataset ds_zip
         |              where substring-before($$t.place.full_name, ",") = $$city.city
         |              and substring-after($$t.place.full_name, ", ") = $$city.state
         |              and not(is-null($$city.county))
         |              return string-concat([$$city.state, "-", $$city.county]) )[0]}
         |)
         |
       """.stripMargin
    )
  }

  def translateQueryToAQL(query: ParsedQuery): AQL = {
    new AQL(
      s"""
         |use dataverse $Dataverse;
         |drop dataset temp_v5os5udpr if exists;
         |create temporary dataset temp_v5os5udpr(type_tweet) primary key id;
         |insert into dataset temp_v5os5udpr (
         |let $$ts_start := datetime("2012-02-17T00:00:00.000Z")
         |let $$ts_end := datetime("2016-03-18T23:59:59.000Z")
         |for $$t in dataset ds_tweets
         |let $$keyword0 := "${query.keyword}"
         |where $$t.place.country = "United States" and $$t.place.place_type = "city"
         |and $$t.create_at >= $$ts_start and $$t.create_at < $$ts_end
         |and contains($$t.text_msg, $$keyword0)
         |return {
         |  "create_at" : $$t.create_at,
         |  "id": $$t.id,
         |  "text_msg" : $$t.text_msg,
         |  "in_reply_to_status" : $$t.in_reply_to_status,
         |  "in_reply_to_user" : $$t.in_reply_to_user,
         |  "favorite_count" : $$t.favorite_count,
         |  "geo_location": $$t.geo_location,
         |  "retweet_count" : $$t.retweet_count,
         |  "lang" : $$t.lang,
         |  "is_retweet": $$t.is_retweet,
         |  "hashtags" :$$t.hashtags,
         |  "user_mentions" : $$t.user_mentions ,
         |  "user" : $$t.user,
         |  "place" : $$t.place,
         |  "state" : substring-after($$t.place.full_name, ", "),
         |  "city" : substring-before($$t.place.full_name, ","),
         |  "county": (for $$city in dataset ds_zip
         |              where substring-before($$t.place.full_name, ",") = $$city.city
         |              and substring-after($$t.place.full_name, ", ") = $$city.state
         |              and not(is-null($$city.county))
         |              return string-concat([$$city.state, "-", $$city.county]) )[0]}
         |)
         |
         |for $$t in dataset temp_v5os5udpr
         |group by $$c := $$t.state with $$t
         |let $$count := count($$t)
         |order by $$count desc
         |return { $$c : $$count };

      """.stripMargin)
    //    |
    //         |for $$t in dataset temp_v5os5udpr
    //         |group by $$c := print-datetime($$t.create_at, "YYYY-MM-DD hh") with $$t
    //         |let $$count := count($$t)
    //         |order by $$c
    //         |return {$$c : $$count };
    //         |
    //         |for $$t in dataset temp_v5os5udpr
    //         |where not(is-null($$t.hashtags))
    //         |for $$h in $$t.hashtags
    //         |group by $$tag := $$h with $$h
    //         |let $$c := count($$h)
    //         |order by $$c desc
    //         |limit 50
    //         |return { $$tag : $$c};
    //         |
    //         |for $$t in dataset temp_v5os5udpr
    //         |limit 100
    //         |return {$$t.user.screen_name : $$t.text_msg};
    //         |
  }

  def createViewMetaTable: AQL = {
    new AQL(
      s"""
         |use dataverse $Dataverse
         |
         |create datatype type$ViewMetaDataset if not exists as open {
         | "dataset": string,
         | "keyword": string,
         | "timeStart": datetime,
         | "timeEnd": datetime
         |}
         |
         |create dataset $ViewMetaDataset(type$ViewMetaDataset) if not exists primary key dataset,keyword;
         |
      """.stripMargin)
  }

  def updateViewMetaTable(dsName: String, keyword: String, interval: Interval): AQL = {
    new AQL(
      s"""
         |use dataverse $Dataverse
         |upsert into $ViewMetaDataset (
         |   "dataset" : $dsName,
         |   "keyword" : $keyword,
         |   "timeStart" : datetime("${TimeFormat.print(interval.getStart)}"),
         |   "timeEnd" : datetime("${TimeFormat.print(interval.getEnd)}")
         |)
       """.stripMargin
    )
  }
}
