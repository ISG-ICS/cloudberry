package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;
import edu.uci.ics.cloudberry.noah.adm.MyTweet.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import twitter4j.GeoLocation;
import twitter4j.Status;
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

    public static StringBuilder admSB = new StringBuilder();
    public static ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    public static Coordinates geoCoordinates;
    public static String Place_Country;
    public static String Place_Country_Code;
    public static String Place_Full_Name;
    public static String Place_Id;
    public static String Place_Name;
    public static String Place_Place_Type;
    public static BoundingBox Place_BoudingBox;


    public static void appendTweetPlainFields(JsonNode rootNode) {
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
    public static StringBuilder sbHashtag=new StringBuilder();
    public static String getHashtags(JsonNode hashtagNode) {
        sbHashtag.delete(0, sbHashtag.length());
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

    public static void appendHashtags(JsonNode rootNode) {
        if (!rootNode.path("entities").isNull() && !rootNode.path("entities").path("hashtags").isNull()) {
            JsonNode hashtagNode = rootNode.path("entities").path("hashtags");
            if (!hashtagNode.equals(null)&&!hashtagNode.toString().equals("[]")) {
                ADM.keyValueToSbWithComma(admSB, HASHTAG, getHashtags(hashtagNode));
            }
        }
    }
    public static StringBuilder sbUserMentions=new StringBuilder();
    public static String getUserMentions(JsonNode userMentionsNode) {
        sbUserMentions.delete(0, sbUserMentions.length());//
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

    public static void appendUserMentsions(JsonNode rootNode) {
        if (!rootNode.path("entities").isNull() && !rootNode.path("entities").path("user_mentions").isNull()) {
            JsonNode userMentionsNode = rootNode.path("entities").path("user_mentions");
            if (userMentionsNode.iterator().hasNext())
                ADM.keyValueToSbWithComma(admSB, USER_MENTION, getUserMentions(userMentionsNode));
        }
    }

    public static void appendPlace(JsonNode rootNode) {
        if (!rootNode.path("place").isNull()) {
            ADM.keyValueToSbWithComma(admSB, PLACE, Place.toADM());
        }
    }
    public static StringBuilder sbGeoCoordinate=new StringBuilder();
    public static void appendGeoCoordinate(JsonNode rootNode) {
        if (!rootNode.path("coordinates").isNull()) {
            try {

                geoCoordinates = objectMapper.readValue(rootNode.path("coordinates").toString(), Coordinates.class);
                sbGeoCoordinate.delete(0,sbGeoCoordinate.length());
                sbGeoCoordinate.append("point(\"" ).append(geoCoordinates.getCoordinates()[0]).append(",").append(geoCoordinates.getCoordinates()[1]).append("\")");
                ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, sbGeoCoordinate.toString());
            } catch (Exception ex) {

            }
        } else if (!rootNode.path("place").isNull() && rootNode.path("place").path("place_type").asText().equals("poi")) {
            try {
                sbGeoCoordinate.delete(0,sbGeoCoordinate.length());
                sbGeoCoordinate.append("point(\"" ).append(Place_BoudingBox.getCoordinates()[0][0][0]).append(",").append(Place_BoudingBox.getCoordinates()[0][0][1]).append("\")");
                ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, sbGeoCoordinate.toString());
            } catch (Exception ex) {

            }
        }
    }

    public static void appendGeoTag(USGeoGnosis gnosis, boolean requireGeoField) {
        try {
            String geoTags = geoTag(gnosis, true);
            if (geoTags != null) {
                ADM.keyValueToSbWithComma(admSB, GEO_TAG, geoTags);
            }
        } catch (Exception ex) {

        }
    }

    public static void appendUser(JsonNode rootNode) {
        ADM.keyValueToSb(admSB, USER, User.toADM(rootNode));
    }

    public static void readPlace(JsonNode placeNode) {
        ResetPlace();//
        if (!placeNode.isNull()) {
            Place_Country = placeNode.path("country").asText();
            Place_Country_Code = placeNode.path("country_code").asText();
            Place_Full_Name = placeNode.path("full_name").asText();
            Place_Id = placeNode.path("id").asText();
            Place_Name = placeNode.path("name").asText();
            Place_Place_Type = placeNode.path("place_type").asText();
            try {
                Place_BoudingBox = objectMapper.readValue(placeNode.path("bounding_box").toString(), BoundingBox.class);
            } catch (Exception ex) {
            }
        }
    }
    public static void ResetPlace()
    {
        Place_Name = "";
        Place_Full_Name = "";
        Place_Place_Type = "";
        Place_Id = "";
        Place_Country_Code = "";
        Place_Country = "";
    }
    //
    public static String toADM(String ln, USGeoGnosis gnosis, boolean requireGeoField) throws IOException {
        admSB.delete(0, admSB.length());
        admSB.append("{");
        try {
            JsonNode rootNode = objectMapper.readTree(ln);
            readPlace(rootNode.path("place"));//
            //
            appendTweetPlainFields(rootNode);
            appendHashtags(rootNode);
            appendUserMentsions(rootNode);
            appendPlace(rootNode);
            appendGeoCoordinate(rootNode);
            appendGeoTag(gnosis, true);
            appendUser(rootNode);
            admSB.append("}");
        } catch (Exception ex) {
            throw ex;
        }
        return admSB.toString();
    }

    public static String toADM(Status status, USGeoGnosis gnosis, boolean requireGeoField) throws UnknownPlaceException {
        String geoTags = geoTag(status, gnosis, requireGeoField);
        if (geoTags == null && requireGeoField)
            return "";
        admSB.delete(0, admSB.length());
        admSB.append("{");
        ADM.keyValueToSbWithComma(admSB, CREATE_AT, ADM.mkDateTimeConstructor(status.getCreatedAt()));
        ADM.keyValueToSbWithComma(admSB, ID, ADM.mkInt64Constructor(status.getId()));
        ADM.keyValueToSbWithComma(admSB, TEXT, ADM.mkQuote(status.getText()));
        ADM.keyValueToSbWithComma(admSB, IN_REPLY_TO_STATUS, ADM.mkInt64Constructor(status.getInReplyToStatusId()));
        ADM.keyValueToSbWithComma(admSB, IN_REPLY_TO_USER, ADM.mkInt64Constructor(status.getInReplyToUserId()));
        ADM.keyValueToSbWithComma(admSB, FAVORITE_COUNT, ADM.mkInt64Constructor(status.getFavoriteCount()));
        ADM.keyValueToSbWithComma(admSB, RETWEET_COUNT, ADM.mkInt64Constructor(status.getRetweetCount()));
        ADM.keyValueToSbWithComma(admSB, LANG, ADM.mkQuote(status.getLang()));
        ADM.keyValueToSbWithComma(admSB, IS_RETWEET, String.valueOf(status.isRetweet()));

        if (status.getHashtagEntities().length > 0) {
            ADM.keyValueToSbWithComma(admSB, HASHTAG, ADM.mkStringSet(status.getHashtagEntities()));
        }
        if (status.getUserMentionEntities().length > 0) {
            ADM.keyValueToSbWithComma(admSB, USER_MENTION, ADM.mkStringSet(status.getUserMentionEntities()));
        }
        if (status.getPlace() != null) {
            ADM.keyValueToSbWithComma(admSB, PLACE, Place.toADM(status.getPlace()));
        }
        if (status.getGeoLocation() != null) {
            ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, ADM.mkPoint(status.getGeoLocation()));
        } else if (status.getPlace() != null && status.getPlace().getPlaceType().equals("poi")) {
            ADM.keyValueToSbWithComma(admSB, GEO_COORDINATE, ADM.mkPoint(status.getPlace().getBoundingBoxCoordinates()[0][0]));
        }
        if (geoTags != null) {
            ADM.keyValueToSbWithComma(admSB, GEO_TAG, geoTags);
        }
        ADM.keyValueToSb(admSB, USER, User.toADM(status.getUser()));
        admSB.append("}");
        return admSB.toString();
    }
    public static StringBuilder sbGeoTag=new StringBuilder();
    public static String geoTag(Status status, USGeoGnosis gnosis, boolean requireGeoField) throws UnknownPlaceException {
        sbGeoTag.delete(0,sbGeoTag.length());
        if (textMatchPlace(sbGeoTag, status, gnosis)) {
            return sbGeoTag.toString();
        }
        GeoLocation location = status.getGeoLocation();
        if (exactPointLookup(sbGeoTag, location, gnosis)) {
            return sbGeoTag.toString();
        }
        if (requireGeoField) {
            throw new UnknownPlaceException("unknown place:" + status.getPlace());
        } else {
            return null;
        }
    }
    public static StringBuilder sbGeoTagLessParam=new StringBuilder();
    public static String geoTag(USGeoGnosis gnosis, boolean requireGeoField) throws UnknownPlaceException {
        sbGeoTagLessParam.delete(0,sbGeoTagLessParam.length());
        if (textMatchPlace(sbGeoTagLessParam, gnosis)) {
            return sbGeoTagLessParam.toString();
        }
        sbGeoTagLessParam.delete(0,sbGeoTagLessParam.length());
        if (exactPointLookup(sbGeoTagLessParam,gnosis)) {
            return sbGeoTagLessParam.toString();
        }
        if (requireGeoField) {
            throw new UnknownPlaceException("unknown place:" + Place_Name);
        } else {
            return null;
        }
    }
    protected static boolean exactPointLookup(StringBuilder sb, USGeoGnosis gnosis) {
        if (geoCoordinates.getCoordinates().length == 0) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info = gnosis.tagPoint(geoCoordinates.getCoordinates()[0],geoCoordinates.getCoordinates()[1]);
        if (info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }
    protected static boolean exactPointLookup(StringBuilder sb, GeoLocation location, USGeoGnosis gnosis) {
        if (location == null) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info = gnosis.tagPoint(location.getLongitude(), location.getLatitude());
        if (info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }
    protected static boolean textMatchPlace(StringBuilder sb, USGeoGnosis gnosis) {
        if (Place_Name == null) {
            return false;
        }
        if (!("United States").equals(Place_Country)) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info;
        switch (Place_Place_Type) {
            case "country":
                return false;
            case "admin": // state level
                return false;
            case "city":
                int index = Place_Full_Name.indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + Place_Full_Name);
                    return false;
                }
                String stateAbbr = Place_Full_Name.substring(index + 1).trim();
                String cityName = Place_Name;
                info = gnosis.tagCity(cityName, stateAbbr);
                break;
            case "neighborhood": // e.g. "The Las Vegas Strip, Paradise"
                index = Place_Full_Name.indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + Place_Full_Name);
                    return false;
                }
                cityName = Place_Full_Name.substring(index + 1).trim();
                int len1 = Place_BoudingBox.getCoordinates().length;
                GeoLocation[][] geoLocations = new GeoLocation[len1][];
                for (int i = 0; i < len1; i++) {
                    int len2 = Place_BoudingBox.getCoordinates()[i].length;
                    geoLocations[i] = new GeoLocation[len2];
                    for (int j = 0; j < len2; j++) {
                        geoLocations[i][j] = new GeoLocation(Place_BoudingBox.getCoordinates()[i][j][1], Place_BoudingBox.getCoordinates()[i][j][0]);
                    }
                }
                info = gnosis.tagNeighborhood(cityName,
                        ADM.coordinates2Rectangle(geoLocations));
                break;
            case "poi": // a point
                double longitude = (Place_BoudingBox.getCoordinates()[0][0][0]);
                double latitude = (Place_BoudingBox.getCoordinates()[0][0][1]);
                info = gnosis.tagPoint(longitude, latitude);
                break;
            default:
                System.err.println("unknown place type:" + Place_Place_Type + Place_Full_Name);
                return false;
        }

        if (info == null || info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }
    protected static boolean textMatchPlace(StringBuilder sb, Status status, USGeoGnosis gnosis) {
        twitter4j.Place place = status.getPlace();
        if (place == null) {
            return false;
        }
        String country = place.getCountry();
        if (!("United States").equals(country)) {
            return false;
        }
        scala.Option<USGeoGnosis.USGeoTagInfo> info;
        String type = place.getPlaceType();
        switch (type) {
            case "country":
                return false;
            case "admin": // state level
                return false;
            case "city":
                int index = place.getFullName().indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + place.getFullName());
                    return false;
                }
                String stateAbbr = place.getFullName().substring(index + 1).trim();
                String cityName = place.getName();
                info = gnosis.tagCity(cityName, stateAbbr);
                break;
            case "neighborhood": // e.g. "The Las Vegas Strip, Paradise"
                index = place.getFullName().indexOf(',');
                if (index < 0) {
                    System.err.println("unknown neighborhood:" + place.getFullName());
                    return false;
                }
                cityName = place.getFullName().substring(index + 1).trim();
                info = gnosis.tagNeighborhood(cityName,
                        ADM.coordinates2Rectangle(place.getBoundingBoxCoordinates()));
                break;
            case "poi": // a point
                double longitude = (place.getBoundingBoxCoordinates())[0][0].getLongitude();
                double latitude = (place.getBoundingBoxCoordinates())[0][0].getLatitude();
                info = gnosis.tagPoint(longitude, latitude);
                break;
            default:
                System.err.println("unknown place type:" + type + status.toString());
                return false;
        }

        if (info == null || info.isEmpty()) {
            return false;
        }
        sb.append(info.get().toString());
        return true;
    }

}
