package edu.uci.ics.cloudberry.noah.adm;

import edu.uci.ics.cloudberry.util.Rectangle;
import org.apache.commons.lang3.StringEscapeUtils;
import twitter4j.GeoLocation;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class ADM {

    public static final SimpleDateFormat ADMDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat ADMTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    //
    public static final SimpleDateFormat srcDateTimeFmt=new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    //
    public static String mkADMConstructor(String constructor, String content) {
        StringBuilder sbConstructor = new StringBuilder();
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

    public static String mkDateTimeConstructor(Date jdate) {
        return "datetime(\"" + ADMDateFormat.format(jdate) + "T" + ADMTimeFormat.format(jdate) + "\")";
    }

    public static String mkDateTimeStringFromTweet(String srcDateTimeString) {
        try {
            Date dt = new java.util.Date(srcDateTimeFmt.parse(srcDateTimeString).getTime());
            StringBuilder sbDateTime = new StringBuilder();
            sbDateTime.append("datetime(\"").append(ADMDateFormat.format(dt)).append("T").append(ADMTimeFormat.format(dt)).append("\")");
            return sbDateTime.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public static String mkDateStringFromTweet(String srcDateTimeString) {
        try {
            Date dt = new java.util.Date(srcDateTimeFmt.parse(srcDateTimeString).getTime());
            StringBuilder sbDateTime = new StringBuilder();
            sbDateTime.append("date(\"").append(ADMTimeFormat.format(dt)).append("\")");
            return sbDateTime.toString();
        } catch (Exception ex) {
            return null;
        }
    }

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

    public static void keyValueToSb(StringBuilder sb, String key, String val) {
        sb.append(mkQuote(key)).append(":").append(val.replaceAll("\\s+", " "));
    }

    public static void keyValueToSbWithComma(StringBuilder sb, String key, String val) {
        keyValueToSb(sb, key, val);
        sb.append(",");
    }

    public static String mkPoint(String lng, String lat) {
        return "point(\"" + lng + "," + lat + "\")";
    }

    public static String mkQuote(String str) {// quote and escape
        StringBuilder sbQuote = new StringBuilder();
        sbQuote.append('"').append(StringEscapeUtils.escapeJava(str)).append('"');
        return sbQuote.toString();
    }

    public static String mkQuoteOnly(String str) {
        StringBuilder sbQuote = new StringBuilder();
        sbQuote.append('"').append(str).append('"');
        return sbQuote.toString();
    }
}
