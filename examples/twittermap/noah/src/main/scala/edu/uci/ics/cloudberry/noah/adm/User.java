package edu.uci.ics.cloudberry.noah.adm;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringEscapeUtils;

public class User {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String SCREEN_NAME = "screen_name";
    public static final String PROFILE_IMAGE_URL = "profile_image_url";
    public static final String LANG = "lang";
    public static final String LOCATION = "location";
    public static final String CREATE_AT = "create_at";
    public static final String DESCRIPTION = "description";
    public static final String FOLLOWERS_COUNT = "followers_count";
    public static final String FRIENDS_COUNT = "friends_count";
    public static final String STATUS_COUNT = "statues_count";

    public static String toADM(JsonNode rootNode) {
        StringBuilder userSB = new StringBuilder();
        userSB.append("{");
        JsonNode userNode = rootNode.path("user");
        if (!userNode.isNull()) {
            ADM.keyValueToSbWithComma(userSB, User.ID, ADM.mkADMConstructor("int64", String.valueOf(userNode.path("id").asLong())));
            ADM.keyValueToSbWithComma(userSB, User.NAME, ADM.mkQuote(userNode.path("name").asText()));
            ADM.keyValueToSbWithComma(userSB, User.SCREEN_NAME, ADM.mkQuoteOnly(userNode.path("screen_name").asText()));
            ADM.keyValueToSbWithComma(userSB, User.PROFILE_IMAGE_URL, ADM.mkQuoteOnly(userNode.path("profile_image_url").asText()));
            ADM.keyValueToSbWithComma(userSB, User.LANG, ADM.mkQuoteOnly(userNode.path("lang").asText()));
            ADM.keyValueToSbWithComma(userSB, User.LOCATION, ADM.mkQuote(userNode.path("location").asText()));
            ADM.keyValueToSbWithComma(userSB, User.CREATE_AT, ADM.mkDateStringFromTweet(userNode.path("created_at").asText()));
            ADM.keyValueToSbWithComma(userSB, User.DESCRIPTION, ADM.mkQuote(StringEscapeUtils.unescapeHtml4(userNode.path("description").asText())));
            ADM.keyValueToSbWithComma(userSB, User.FOLLOWERS_COUNT, String.valueOf(userNode.path("followers_count").asInt()));
            ADM.keyValueToSbWithComma(userSB, User.FRIENDS_COUNT, String.valueOf(userNode.path("friends_count").asInt()));
            ADM.keyValueToSb(userSB, User.STATUS_COUNT, String.valueOf(userNode.path("statuses_count").asInt()));
        }
        userSB.append("}");
        return userSB.toString();
    }
}
