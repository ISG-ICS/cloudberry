package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;
import edu.uci.ics.cloudberry.noah.adm.MyTweet.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import twitter4j.GeoLocation;

import java.io.IOException;
import java.util.*;


public class Tweet {
    public static String CREATE_AT = "create_at";
    public static String ID = "id";
    public static String TEXT = "text";
    public static String IN_REPLY_TO_STATUS = "in_reply_to_status";
    public static String IN_REPLY_TO_USER = "in_reply_to_user";
    public static String FAVORITE_COUNT = "favorite_count";
    public static String GEO_TAG = "geo_tag";
    public static String GEO_COORDINATE = "coordinate";
    public static String RETWEET_COUNT = "retweet_count";
    public static String LANG = "lang";
    public static String IS_RETWEET = "is_retweet";
    public static String HASHTAG = "hashtags";
    public static String USER_MENTION = "user_mentions";
    public static String USER = "user";
    public static String PLACE = "place";

    public static void appendTweetPlainFields(JsonNode rootNode, StringBuilder admSB) {
        ADM.keyValueToSbWithComma(admSB, CREATE_AT, ADM.mkDateTimeStringFromTweet(rootNode.path("created_at").asText()));
        ADM.keyValueToSbWithComma(admSB, ID, ADM.mkInt64Constructor(rootNode.path("id").asLong()));
        ADM.keyValueToSbWithComma(admSB, TEXT, ADM.mkQuote(StringEscapeUtils.unescapeHtml4(rootNode.path("text").asText())));
        if (rootNode.path("in_reply_to_status_id").isNull())
            ADM.keyValueToSbWithComma(admSB, IN_REPLY_TO_STATUS, ADM.mkInt64Constructor(-1));
        else
            ADM.keyValueToSbWithComma(admSB, IN_REPLY_TO_STATUS, ADM.mkInt64Constructor(rootNode.path("in_reply_to_status_id").asLong()));
        if (rootNode.path("in_reply_to_user_id").isNull())
            ADM.keyValueToSbWithComma(admSB, IN_REPLY_TO_USER, ADM.mkInt64Constructor(-1));
        else
            ADM.keyValueToSbWithComma(admSB, IN_REPLY_TO_USER, ADM.mkInt64Constructor(rootNode.path("in_reply_to_user_id").asLong()));
        ADM.keyValueToSbWithComma(admSB, FAVORITE_COUNT, ADM.mkInt64Constructor(rootNode.path("favorite_count").asLong()));
        ADM.keyValueToSbWithComma(admSB, RETWEET_COUNT, ADM.mkInt64Constructor(rootNode.path("retweet_count").asLong()));
        ADM.keyValueToSbWithComma(admSB, LANG, ADM.mkQuoteOnly(rootNode.path("lang").asText()));
        ADM.keyValueToSbWithComma(admSB, IS_RETWEET, String.valueOf(rootNode.path("retweeted").asBoolean()));
    }


    public static String getHashtags(JsonNode hashtagNode) {
        StringBuilder sbHashtag=new StringBuilder();
        sbHashtag.append("{{");
        int i = 0;
        Iterator<JsonNode> elements = hashtagNode.elements();
        while (elements.hasNext()) {
            if (i > 0) {
                sbHashtag.append(",");
            }
            sbHashtag.append(ADM.mkQuote(elements.next().path("text").asText()));
            i++;
        }
        sbHashtag.append("}}");
        return sbHashtag.toString();
    }

    public static void appendHashtags(JsonNode rootNode, StringBuilder admSB) {
        if (!rootNode.path("entities").isNull() && !rootNode.path("entities").path("hashtags").isNull()) {
            JsonNode hashtagNode = rootNode.path("entities").path("hashtags");
            if (!hashtagNode.equals(null) && !hashtagNode.toString().equals("[]")) {
                ADM.keyValueToSbWithComma(admSB, HASHTAG, getHashtags(hashtagNode));
            }
        }
    }

    public static String getUserMentions(JsonNode userMentionsNode) {
        StringBuilder sbUserMentions=new StringBuilder();
        sbUserMentions.append("{{");
        int i = 0;
        Iterator<JsonNode> elements = userMentionsNode.elements();
        while (elements.hasNext()) {
            if (i > 0) {
                sbUserMentions.append(",");
            }
            sbUserMentions.append(elements.next().path("id").asText());
            i++;
        }
        sbUserMentions.append("}}");
        return sbUserMentions.toString();
    }

    public static void appendUserMentsions(JsonNode rootNode, StringBuilder admSB) {
        if (!rootNode.path("entities").isNull() && !rootNode.path("entities").path("user_mentions").isNull()) {
            JsonNode userMentionsNode = rootNode.path("entities").path("user_mentions");
            if (userMentionsNode.iterator().hasNext())
                ADM.keyValueToSbWithComma(admSB, USER_MENTION, getUserMentions(userMentionsNode));
        }
    }

    public static void appendGeoCoordinate(JsonNode rootNode, StringBuilder admSB, Places places, Coordinates coordinates) {
        StringBuilder sbGeoCoordinate=new StringBuilder();
        if (coordinates.getCoordinates().length > 0) {
            sbGeoCoordinate.append("point(\"").append(coordinates.getCoordinates()[0]).append(",").append(coordinates.getCoordinates()[1]).append("\")");
            ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, sbGeoCoordinate.toString());
        } else if (!places.getName().equals("") && places.getPlace_type().equals("poi")) {
            sbGeoCoordinate.append("point(\"").append(places.getBounding_box().getCoordinates()[0][0][0]).append(",").append(places.getBounding_box().getCoordinates()[0][0][1]).append("\")");
            ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, sbGeoCoordinate.toString());
        }
    }

    public static void appendGeoTag(USGeoGnosis gnosis, boolean requireGeoField, StringBuilder admSB, Places places, Coordinates coordinates) throws UnknownPlaceException {
        try {
            String geoTags = geoTag(gnosis, true, places, coordinates);
            if (geoTags != null) {
                ADM.keyValueToSbWithComma(admSB, GEO_TAG, geoTags);
            }
        } catch (UnknownPlaceException ex) {
            throw ex;
        }
    }

    public static void appendUser(JsonNode rootNode, StringBuilder admSB) {
        ADM.keyValueToSb(admSB, USER, User.toADM(rootNode));
    }

    //
    public static String toADM(String ln, USGeoGnosis gnosis, boolean requireGeoField) throws UnknownPlaceException, IOException {
        StringBuilder admSB = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Places places = new Places();
        Coordinates coordinates = new Coordinates();
        admSB.append("{");
        try {
            JsonNode rootNode = objectMapper.readTree(ln);
            //
            appendTweetPlainFields(rootNode, admSB);
            appendHashtags(rootNode, admSB);
            appendUserMentsions(rootNode, admSB);
            if (!rootNode.path("place").isNull()) {
                places = objectMapper.readValue(rootNode.path("place").toString(), Places.class);
                ADM.keyValueToSbWithComma(admSB, PLACE, Place.toADM(places));
            }
            if (!rootNode.path("coordinates").isNull()) {
                coordinates = objectMapper.readValue(rootNode.path("coordinates").toString(), Coordinates.class);
                appendGeoCoordinate(rootNode, admSB, places, coordinates);
            }
            appendGeoTag(gnosis, true, admSB, places, coordinates);
            appendUser(rootNode, admSB);
            admSB.append("}");
        } catch (Exception ex) {
            throw ex;
        }
        return admSB.toString();
    }

    public static String geoTag(USGeoGnosis gnosis, boolean requireGeoField, Places places, Coordinates coordinates) throws UnknownPlaceException {
        StringBuilder sbGeoTagLessParam = new StringBuilder();
        if (textMatchPlace(sbGeoTagLessParam, gnosis, places)) {
            return sbGeoTagLessParam.toString();
        }
        sbGeoTagLessParam.delete(0, sbGeoTagLessParam.length());
        if (exactPointLookup(sbGeoTagLessParam, gnosis, coordinates)) {
            return sbGeoTagLessParam.toString();
        }
        if (requireGeoField) {
            throw new UnknownPlaceException("unknown place:" + places.toString());
        } else {
            return null;
        }
    }

    protected static boolean exactPointLookup(StringBuilder sb, USGeoGnosis gnosis, Coordinates coordinates) {
        if (coordinates.getCoordinates().length == 0) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info = gnosis.tagPoint(coordinates.getCoordinates()[0], coordinates.getCoordinates()[1]);
        if (info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }

    protected static boolean textMatchPlace(StringBuilder sb, USGeoGnosis gnosis, Places places) {
        if (places.getName() == null) {
            return false;
        }
        if (!("United States").equals(places.getCountry())) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info;
        switch (places.getPlace_type()) {
            case "country":
                return false;
            case "admin": // state level
                return false;
            case "city":
                int index = places.getFull_name().indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + places.getFull_name());
                    return false;
                }
                String stateAbbr = places.getFull_name().substring(index + 1).trim();
                String cityName = places.getName();
                info = gnosis.tagCity(cityName, stateAbbr);
                break;
            case "neighborhood": // e.g. "The Las Vegas Strip, Paradise"
                index = places.getFull_name().indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + places.getFull_name());
                    return false;
                }
                cityName = places.getFull_name().substring(index + 1).trim();
                int len1 = places.getBounding_box().getCoordinates().length;
                GeoLocation[][] geoLocations = new GeoLocation[len1][];
                for (int i = 0; i < len1; i++) {
                    int len2 = places.getBounding_box().getCoordinates()[i].length;
                    geoLocations[i] = new GeoLocation[len2];
                    for (int j = 0; j < len2; j++) {
                        geoLocations[i][j] = new GeoLocation(places.getBounding_box().getCoordinates()[i][j][1], places.getBounding_box().getCoordinates()[i][j][0]);
                    }
                }
                info = gnosis.tagNeighborhood(cityName,
                        ADM.coordinates2Rectangle(geoLocations));
                break;
            case "poi": // a point
                double longitude = (places.getBounding_box().getCoordinates()[0][0][0]);
                double latitude = (places.getBounding_box().getCoordinates()[0][0][1]);
                info = gnosis.tagPoint(longitude, latitude);
                break;
            default:
                System.err.println("unknown place type:" + places.getPlace_type() + places.getFull_name());
                return false;
        }

        if (info == null || info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }
}
