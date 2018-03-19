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
    public static String GEO_COORDINATE = "coordinate";
    public static String RETWEET_COUNT = "retweet_count";
    public static String LANG = "lang";
    public static String IS_RETWEET = "is_retweet";
    public static String HASHTAG = "hashtags";
    public static String USER_MENTION = "user_mentions";
    public static String USER = "user";
    public static String PLACE = "place";

    public static StringBuilder admSB = new StringBuilder();


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
