package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;
import twitter4j.GeoLocation;
import twitter4j.Status;

public class Tweet {
    public static String CREATE_AT = "create_at";
    public static String ID = "id";
    public static String TEXT = "text";
    public static String IN_REPLY_TO_STATUS = "in_reply_to_status";
    public static String IN_REPLY_TO_USER = "in_reply_to_user";
    public static String FAVORITE_COUNT = "favorite_count";
    public static String GEO_TAG = "geo_tag";
    public static String RETWEET_COUNT = "retweet_count";
    public static String LANG = "lang";
    public static String IS_RETWEET = "is_retweet";
    public static String HASHTAG = "hashtags";
    public static String USER_MENTION = "user_mentions";
    public static String USER = "user";
    public static String PLACE = "place";

    public static String toADM(Status status, USGeoGnosis gnosis) {
        String geoTags = geoTag(status, gnosis);
        if (geoTags.length() < 1) {
            return geoTags;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        ADM.keyValueToSbWithComma(sb, CREATE_AT, ADM.mkDateTimeConstructor(status.getCreatedAt()));
        ADM.keyValueToSbWithComma(sb, ID, ADM.mkInt64Constructor(status.getId()));
        ADM.keyValueToSbWithComma(sb, TEXT, ADM.mkQuote(status.getText()));
        ADM.keyValueToSbWithComma(sb, IN_REPLY_TO_STATUS, ADM.mkInt64Constructor(status.getInReplyToStatusId()));
        ADM.keyValueToSbWithComma(sb, IN_REPLY_TO_USER, ADM.mkInt64Constructor(status.getInReplyToUserId()));
        ADM.keyValueToSbWithComma(sb, FAVORITE_COUNT, ADM.mkInt64Constructor(status.getFavoriteCount()));
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

        ADM.keyValueToSbWithComma(sb, PLACE, Place.toADM(status.getPlace()));

        ADM.keyValueToSb(sb, GEO_TAG, geoTags);
        sb.append("}");
        return sb.toString();
    }

    public static String GEO_LOCATION = "geo_location";
    public static String CITY_ID = "city_id";
    public static String CITY_NAME = "city_name";
    public static String COUNTY_ID = "county_id";
    public static String COUNTY_NAME = "county_name";
    public static String STATE_ID = "state_id";
    public static String STATE_NAME = "state_name";
    public static String UNKNOWN = "unknown";

    public static String geoTag(Status status, USGeoGnosis gnosis) {
        StringBuilder sb = new StringBuilder();
        GeoLocation location = status.getGeoLocation();
        if (textMatchPlace(sb, status, gnosis)) {

        } else if (exactPointLookup(sb, location, gnosis)) {
        } else {
            System.err.println("unknown place:" + status);
        }
        return sb.toString();
    }

    private static boolean exactPointLookup(StringBuilder sb, GeoLocation location, USGeoGnosis gnosis) {
        return false;
    }

    private static boolean textMatchPlace(StringBuilder sb, Status status, USGeoGnosis gnosis) {
        twitter4j.Place place = status.getPlace();
        if (place == null) {
            return false;
        }
        String country = place.getCountry();
        if (("United States").equals(country)){
            return false;
        }
        String type = place.getPlaceType();
        switch (type) {
            case "country":
                return false;
            case "admin": // state level
                break;
            case "city":
                break;
            case "neighborhood": // e.g. "The Las Vegas Strip, Paradise"
                int index = place.getFullName().indexOf(',');
                if (index < 0){
                    System.err.println("unknown neighborhood:"  + place.getFullName());
                    return false;
                }
                String cityName = place.getFullName().substring(index+1);
                USGeoGnosis.USGeoTagInfo info = gnosis.tagNeighborhood(cityName,
                        ADM.coordinates2Rectangle(place.getBoundingBoxCoordinates()));
                break;
            case "poi": // a point
                break;
            default:
                System.err.println("unknown place type:" + type + status.toString());
                return false;
        }
        return false;
    }

}
