package edu.uci.ics.twitter.asterix.adm;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;

import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.UserMentionEntity;

public class ADM {

    public static final SimpleDateFormat ADMDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat ADMTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public static String mkADMConstructor(String constructor, String content) {
        return constructor + "(\"" + content + "\")";
    }

    public static String mkInt64Constructor(long value) {
        return mkADMConstructor("int64", String.valueOf(value));
    }

    public static String mkDateConstructor(Date jdate) {
        return "date(\"" + ADMDateFormat.format(jdate) + "\")";
    }

    public static String mkDateTimeConstructor(Date jdate) {
        return "datetime(\"" + ADMDateFormat.format(jdate) + "T" + ADMTimeFormat.format(jdate) + "\")";
    }

    public static String mkRectangleConstructor(GeoLocation[][] boundingBoxCoordinates)
            throws IllegalArgumentException {
        StringBuilder sb = new StringBuilder("rectangle");
        if (boundingBoxCoordinates.length != 1 || boundingBoxCoordinates[0].length != 4) {
            throw new IllegalArgumentException("unknown boundingBoxCoordinates");
        }
        if (boundingBoxCoordinates[0][0].getLongitude() != boundingBoxCoordinates[0][1].getLongitude()
                || boundingBoxCoordinates[0][1].getLatitude() != boundingBoxCoordinates[0][2].getLatitude()
                || boundingBoxCoordinates[0][2].getLongitude() != boundingBoxCoordinates[0][3].getLongitude()
                || boundingBoxCoordinates[0][3].getLatitude() != boundingBoxCoordinates[0][0].getLatitude()) {
            throw new IllegalArgumentException("boundingBoxCoordinates is not a rectangle shape");
        }

        if (boundingBoxCoordinates[0][0].getLongitude() > 0 || boundingBoxCoordinates[0][2].getLongitude() > 0) {
            throw new IllegalArgumentException(
                    "I found you!" + boundingBoxCoordinates[0][0] + " " + boundingBoxCoordinates[0][2]);
        }

        if (boundingBoxCoordinates[0][0].getLongitude() > boundingBoxCoordinates[0][2].getLongitude() ||
                boundingBoxCoordinates[0][0].getLatitude() > boundingBoxCoordinates[0][2].getLatitude()) {
            throw new IllegalArgumentException(
                    "Not a good Rectangle: " + boundingBoxCoordinates[0][0] + " " + boundingBoxCoordinates[0][2]);
        }

        sb.append("(\"").append(boundingBoxCoordinates[0][0].getLongitude()).append(',')
                .append(boundingBoxCoordinates[0][0].getLatitude())
                .append(' ')
                .append(boundingBoxCoordinates[0][2].getLongitude()).append(',')
                .append(boundingBoxCoordinates[0][2].getLatitude())
                .append("\")");
        return sb.toString();
    }

    public static void keyValueToSb(StringBuilder sb, String key, String val) {
        sb.append(mkQuote(key)).append(":").append(val.replaceAll("\\s+", " "));
    }

    public static void keyValueToSbWithComma(StringBuilder sb, String key, String val) {
        keyValueToSb(sb, key, val);
        sb.append(",");
    }

    public static String mkPoint(GeoLocation geoLocation) {
        return "point(\"" + geoLocation.getLongitude() + "," + geoLocation.getLatitude() + "\")";
    }

    public static String mkStringSet(HashtagEntity[] hashtagEntities) {
        StringBuilder sb = new StringBuilder();
        sb.append("{{");
        for (int i = 0; i < hashtagEntities.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(mkQuote(hashtagEntities[i].getText()));
        }
        sb.append("}}");
        return sb.toString();
    }

    public static String mkStringSet(UserMentionEntity[] userMentionEntities) {
        StringBuilder sb = new StringBuilder();
        sb.append("{{");
        for (int i = 0; i < userMentionEntities.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(userMentionEntities[i].getId());
        }
        sb.append("}}");
        return sb.toString();
    }

    public static String mkQuote(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(StringEscapeUtils.escapeJava(str)).append('"');
        return sb.toString();
    }
}
