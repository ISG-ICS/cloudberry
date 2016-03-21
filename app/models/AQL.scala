package models

import actors.ParsedQuery

class AQL(val statement: String) {

}

object AQL {
  def toKeywordSelection(query: Any): AQL = ???

  def toTimeIntervalSelection(query: Any): AQL = ???

  def toSpatialSelection(query: Any): AQL = ???

  def createTempTable(): AQL = ???

  def translateQueryToAQL(query: ParsedQuery): AQL = {
    new AQL(
      s"""
         |use dataverse twitter;
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
         |
         |for $$t in dataset temp_v5os5udpr
         |group by $$c := print-datetime($$t.create_at, "YYYY-MM-DD hh") with $$t
         |let $$count := count($$t)
         |order by $$c
         |return {$$c : $$count };
         |
         |for $$t in dataset temp_v5os5udpr
         |where not(is-null($$t.hashtags))
         |for $$h in $$t.hashtags
         |group by $$tag := $$h with $$h
         |let $$c := count($$h)
         |order by $$c desc
         |limit 50
         |return { $$tag : $$c};
         |
         |for $$t in dataset temp_v5os5udpr
         |limit 100
         |return {$$t.user.screen_name : $$t.text_msg};
         |
      """.stripMargin)
  }
}
