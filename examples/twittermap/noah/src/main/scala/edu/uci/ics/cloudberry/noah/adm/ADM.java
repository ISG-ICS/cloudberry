package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.util.Rectangle;
import org.apache.commons.lang3.StringEscapeUtils;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.UserMentionEntity;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class ADM {

    public static final SimpleDateFormat ADMDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat ADMTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public static StringBuilder sbConstructor=new StringBuilder();
    public static String mkADMConstructor(String constructor, String content) {
        sbConstructor.delete(0,sbConstructor.length());
        sbConstructor.append(constructor).append("(\"").append(content).append("\")");
        return sbConstructor.toString();
    }

    public static String mkInt64Constructor(long value) {
        return mkADMConstructor("int64", String.valueOf(value));
    }

    public static String mkInt8Constructor(String value) {
        return mkADMConstructor("int8", value);
    }

    public static String mkInt32Constructor(String value) {
        return mkADMConstructor("int32", value);
    }

    public static String mkFloatConstructor(String value) {
        return mkADMConstructor("float", (Float.toString(Float.parseFloat(value))) + "f");
    }

    public static String mkDateConstructor(Date jdate) {
        return "date(\"" + ADMDateFormat.format(jdate) + "\")";
    }

    public static String mkDateTimeConstructor(Date jdate) {
        return "datetime(\"" + ADMDateFormat.format(jdate) + "T" + ADMTimeFormat.format(jdate) + "\")";
    }

    public static StringBuilder sbDate = new StringBuilder();

    public static Rectangle coordinates2Rectangle(GeoLocation[][] boundingBoxCoordinates) {
        if (boundingBoxCoordinates.length != 1 || boundingBoxCoordinates[0].length != 4) {
            throw new IllegalArgumentException("unknown boundingBoxCoordinates");
        }
        // Twitter has some wield format historically, though it still rectangle, but it is not always
        // in (sw, se, ne,nw) order
        double swLog = Collections.min(Arrays.asList(boundingBoxCoordinates[0][0].getLongitude(),
                boundingBoxCoordinates[0][1].getLongitude(),
                boundingBoxCoordinates[0][2].getLongitude(),
                boundingBoxCoordinates[0][3].getLongitude()));
        double swLat = Collections.min(Arrays.asList(boundingBoxCoordinates[0][0].getLatitude(),
                boundingBoxCoordinates[0][1].getLatitude(),
                boundingBoxCoordinates[0][2].getLatitude(),
                boundingBoxCoordinates[0][3].getLatitude()));
        double neLog = Collections.max(Arrays.asList(boundingBoxCoordinates[0][0].getLongitude(),
                boundingBoxCoordinates[0][1].getLongitude(),
                boundingBoxCoordinates[0][2].getLongitude(),
                boundingBoxCoordinates[0][3].getLongitude()));
        double neLat = Collections.max(Arrays.asList(boundingBoxCoordinates[0][0].getLatitude(),
                boundingBoxCoordinates[0][1].getLatitude(),
                boundingBoxCoordinates[0][2].getLatitude(),
                boundingBoxCoordinates[0][3].getLatitude()));

        // AsterixDB is unhappy with this kind of point "rectangular"
        if (swLog == neLog && swLat == neLat) {
            swLog = neLog - 0.0000001;
            swLat = neLat - 0.0000001;
        }
        if (swLog > neLog || swLat > neLat) {
            throw new IllegalArgumentException(
                    "Not a good Rectangle: " + "sw:" + swLog + "," + swLat + ", ne:" + neLog + "," + neLat);
        }
        return new Rectangle(swLog, swLat, neLog, neLat);
    }
    public static StringBuilder sbRectangleConstructor=new StringBuilder();
    public static String mkRectangleConstructor(GeoLocation[][] boundingBoxCoordinates)
            throws IllegalArgumentException {
        sbRectangleConstructor.delete(0,sbRectangleConstructor.length());
        sbRectangleConstructor.append("rectangle");
        Rectangle rectangle = coordinates2Rectangle(boundingBoxCoordinates);
        sbRectangleConstructor.append("(\"").append(rectangle.swLog()).append(',')
                .append(rectangle.swLat())
                .append(' ')
                .append(rectangle.neLog()).append(',')
                .append(rectangle.neLat())
                .append("\")");
        return sbRectangleConstructor.toString();
    }

    public static void keyValueToSb(StringBuilder sb, String key, String val) {
        sb.append(mkQuote(key)).append(":").append(val.replaceAll("\\s+", " "));
    }

    public static void keyValueToSbWithComma(StringBuilder sb, String key, String val) {
        keyValueToSb(sb, key, val);
        sb.append(",");
    }
    public static StringBuilder sbPoint=new StringBuilder();
    public static String mkPoint(GeoLocation geoLocation) {
        sbPoint.delete(0,sbPoint.length());
        sbPoint.append("point(\"" ).append(geoLocation.getLongitude()).append(",").append(geoLocation.getLatitude()).append("\")");
        return  sbPoint.toString();
    }

    public static String mkPoint(String lng, String lat) {
        return "point(\"" + lng + "," + lat + "\")";
    }
    public static StringBuilder sbStringSetHash=new StringBuilder();
    public static String mkStringSet(HashtagEntity[] hashtagEntities) {
        sbStringSetHash.delete(0,sbStringSetHash.length());
        sbStringSetHash.append("{{");
        for (int i = 0; i < hashtagEntities.length; i++) {
            if (i > 0) {
                sbStringSetHash.append(',');
            }
            sbStringSetHash.append(mkQuote(hashtagEntities[i].getText()));
        }
        sbStringSetHash.append("}}");
        return sbStringSetHash.toString();
    }
    public static StringBuilder sbStringSetUserMention=new StringBuilder();
    public static String mkStringSet(UserMentionEntity[] userMentionEntities) {
        sbStringSetUserMention.delete(0,sbStringSetUserMention.length());
        sbStringSetUserMention.append("{{");
        for (int i = 0; i < userMentionEntities.length; i++) {
            if (i > 0) {
                sbStringSetUserMention.append(',');
            }
            sbStringSetUserMention.append(userMentionEntities[i].getId());
        }
        sbStringSetUserMention.append("}}");
        return sbStringSetUserMention.toString();
    }
    public static StringBuilder sbQuote=new StringBuilder();
    public static String mkQuote(String str) {
        sbQuote.delete(0,sbQuote.length());
        sbQuote.append('"').append(StringEscapeUtils.escapeJava(str)).append('"');
        return sbQuote.toString();
    }
}
