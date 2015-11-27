package edu.uci.ics.twitter.asterix.adm;

import twitter4j.Status;

public class Tweet {
    public static String CREATE_AT = "create_at";
    public static String ID = "id";
    public static String TEXT = "text_msg";
    public static String IN_REPLY_TO_STATUS = "in_reply_to_status";
    public static String IN_REPLY_TO_USER = "in_reply_to_user";
    public static String FAVORITE_COUNT = "favorite_count";
    public static String GEO_LOCATION = "geo_location";
    public static String RETWEET_COUNT = "retweet_count";
    public static String LANG = "lang";
    public static String IS_RETWEET = "is_retweet";
    public static String HASHTAG = "hashtags";
    public static String USER_MENTION = "user_mentions";
    public static String USER = "user";
    public static String PLACE = "place";

    public static String toADM(Status status) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        ADM.keyValueToSbWithComma(sb, CREATE_AT, ADM.mkDateTimeConstructor(status.getCreatedAt()));
        ADM.keyValueToSbWithComma(sb, ID, ADM.mkInt64Constructor(status.getId()));
        ADM.keyValueToSbWithComma(sb, TEXT, ADM.mkQuote(status.getText()));
        ADM.keyValueToSbWithComma(sb, IN_REPLY_TO_STATUS, ADM.mkInt64Constructor(status.getInReplyToStatusId()));
        ADM.keyValueToSbWithComma(sb, IN_REPLY_TO_USER, ADM.mkInt64Constructor(status.getInReplyToUserId()));
        ADM.keyValueToSbWithComma(sb, FAVORITE_COUNT, ADM.mkInt64Constructor(status.getFavoriteCount()));
        if (status.getGeoLocation() != null) {
            ADM.keyValueToSbWithComma(sb, GEO_LOCATION, ADM.mkPoint(status.getGeoLocation()));
        }
        ADM.keyValueToSbWithComma(sb, RETWEET_COUNT, ADM.mkInt64Constructor(status.getRetweetCount()));
        ADM.keyValueToSbWithComma(sb, LANG, ADM.mkQuote(status.getLang()));
        ADM.keyValueToSbWithComma(sb, IS_RETWEET, String.valueOf(status.isRetweet()));

        if (status.getHashtagEntities().length > 0) {
            ADM.keyValueToSbWithComma(sb, HASHTAG, ADM.mkStringSet(status.getHashtagEntities()));
        }
        if (status.getUserMentionEntities().length > 0) {
            ADM.keyValueToSbWithComma(sb, USER_MENTION, ADM.mkStringSet(status.getUserMentionEntities()));
        }
        ADM.keyValueToSbWithComma(sb, USER, User.toADM(status.getUser()));
        ADM.keyValueToSb(sb, PLACE, Place.toADM(status.getPlace()));
        sb.append("}");
        return sb.toString();
    }

}
