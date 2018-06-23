package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;
import edu.uci.ics.cloudberry.noah.adm.mytweet.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.cloudberry.util.Rectangle;

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
        ADM.keyValueToSbWithComma(admSB, TEXT, ADM.mkQuoteOnly(rootNode.path("text").asText()));
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
        StringBuilder sbHashtag = new StringBuilder();
        sbHashtag.append("{{");
        int i = 0;
        Iterator<JsonNode> elements = hashtagNode.elements();
        while (elements.hasNext()) {
            if (i > 0) {
                sbHashtag.append(",");
            }
            sbHashtag.append(ADM.mkQuoteOnly(elements.next().path("text").asText()));
            i++;
        }
        sbHashtag.append("}}");
        return sbHashtag.toString();
    }

    public static void appendHashtags(JsonNode rootNode, StringBuilder admSB) {
        if (!rootNode.path("entities").isNull() && !rootNode.path("entities").path("hashtags").isNull()) {
            JsonNode hashtagNode = rootNode.path("entities").path("hashtags");
            if (hashtagNode != null && !hashtagNode.toString().equals("[]")) {
                ADM.keyValueToSbWithComma(admSB, HASHTAG, getHashtags(hashtagNode));
            }
        }
    }

    public static String getUserMentions(JsonNode userMentionsNode) {
        StringBuilder sbUserMentions = new StringBuilder();
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

    public static void appendGeoCoordinate(JsonNode rootNode, StringBuilder admSB, MyPlace places, Coordinate coordinate) {
        StringBuilder sbGeoCoordinate = new StringBuilder();
        if (!coordinate.getcLat().equals("")) {
            sbGeoCoordinate.append("point(\"").append(coordinate.getcLong()).append(",").append(coordinate.getcLat()).append("\")");
            ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, sbGeoCoordinate.toString());
        } else if (!places.getName().equals("") && places.getPlacetype().equals("poi")) {
            sbGeoCoordinate.append("point(\"").append(places.getaLat()).append(",").append(places.getaLong()).append("\")");
            ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, sbGeoCoordinate.toString());
        }
    }

    public static void appendGeoTag(USGeoGnosis gnosis, boolean requireGeoField, StringBuilder admSB, MyPlace places, Coordinate coordinate) throws UnknownPlaceException {
        try {
            String geoTags = geoTag(gnosis, true, places, coordinate);
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

    public static boolean readPlaces(JsonNode placeNode, MyPlace place) {
        place.setCountry(placeNode.path("country").asText());
        place.setId(placeNode.path("id").asText());
        place.setName(placeNode.path("name").asText());
        place.setFullname(placeNode.path("full_name").asText());
        place.setCountrycode(placeNode.path("country_code").asText());
        place.setPlacetype(placeNode.path("place_type").asText());
        try {
            //
            if (placeNode.path("bounding_box").path("coordinates").elements().hasNext()) {
                String coordStr = placeNode.path("bounding_box").path("coordinates").elements().next().toString();
                String[] lats_longs = coordStr.split("\\[\\[|\\]\\]|\\],\\[|,");
                if (lats_longs.length == 9) {
                    place.setaLong(Double.parseDouble(lats_longs[2]));
                    place.setaLat(Double.parseDouble(lats_longs[1]));
                    place.setbLong(Double.parseDouble(lats_longs[6]));
                    place.setbLat(Double.parseDouble(lats_longs[5]));
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
            //
        } catch (Exception ex) {
            return false;
        }
    }

    //
    public static boolean readCoordinate(JsonNode coordNode, Coordinate coordinate) {
        String[] lat_long = coordNode.path("coordinates").toString().split("\\[|\\]|,");
        if (lat_long.length == 3) {
            coordinate.setcLat(lat_long[2]);
            coordinate.setcLong(lat_long[1]);
            return true;
        }
        return false;
    }

    public static String toADM(String ln, USGeoGnosis gnosis, boolean requireGeoField) throws UnknownPlaceException, IOException {
        StringBuilder admSB = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MyPlace place = new MyPlace();//field: tweet->place
        Coordinate coordinate = new Coordinate();
        admSB.append("{");
        try {
            JsonNode rootNode = objectMapper.readTree(ln);
            //
            appendTweetPlainFields(rootNode, admSB);
            appendHashtags(rootNode, admSB);
            appendUserMentsions(rootNode, admSB);
            if (!rootNode.path("place").isNull() && readPlaces(rootNode.path("place"), place)) {
                ADM.keyValueToSbWithComma(admSB, PLACE, place.toADM());
            }
            if (!rootNode.path("coordinates").isNull() && readCoordinate(rootNode.path("coordinates"), coordinate)) {
                appendGeoCoordinate(rootNode, admSB, place, coordinate);
            }
            appendGeoTag(gnosis, true, admSB, place, coordinate);
            appendUser(rootNode, admSB);
            admSB.append("}");
        } catch (Exception ex) {
            throw ex;
        }
        return admSB.toString();
    }

    public static String geoTag(USGeoGnosis gnosis, boolean requireGeoField, MyPlace place, Coordinate coordinate) throws UnknownPlaceException {
        StringBuilder sbGeoTag = new StringBuilder();
        if (textMatchPlace(sbGeoTag, gnosis, place)) {
            return sbGeoTag.toString();
        }
        sbGeoTag.delete(0, sbGeoTag.length());
        if (exactPointLookup(sbGeoTag, gnosis, coordinate)) {
            return sbGeoTag.toString();
        }
        if (requireGeoField) {
            throw new UnknownPlaceException("unknown place:" + place.toString());
        } else {
            return null;
        }
    }

    protected static boolean exactPointLookup(StringBuilder sb, USGeoGnosis gnosis, Coordinate coordinate) {
        if (coordinate.getcLat()==null||coordinate.getcLat().equals("")) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info = gnosis.tagPoint(Double.parseDouble(coordinate.getcLong()), Double.parseDouble(coordinate.getcLat()));
        if (info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }

    protected static boolean textMatchPlace(StringBuilder sb, USGeoGnosis gnosis, MyPlace place) {
        if (place.getName() == null) {
            return false;
        }
        if (!("United States").equals(place.getCountry())) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info;
        switch (place.getPlacetype()) {
            case "country":
                return false;
            case "admin": // state level
                return false;
            case "city":
                int index = place.getFullname().indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + place.getFullname());
                    return false;
                }
                String stateAbbr = place.getFullname().substring(index + 1).trim();
                String cityName = place.getName();
                info = gnosis.tagCity(cityName, stateAbbr);
                break;
            case "neighborhood": // e.g. "The Las Vegas Strip, Paradise"
                index = place.getFullname().indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + place.getFullname());
                    return false;
                }
                cityName = place.getFullname().substring(index + 1).trim();
                info = gnosis.tagNeighborhood(cityName, new Rectangle(place.getaLat(), place.getaLat(), place.getbLat(), place.getbLat()));
                break;
            case "poi": // a point
                double longitude = (place.getaLong());
                double latitude = (place.getaLat());
                info = gnosis.tagPoint(longitude, latitude);
                break;
            default:
                System.err.println("unknown place type:" + place.getPlacetype() + place.getFullname());
                return false;
        }

        if (info == null || info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }
}
