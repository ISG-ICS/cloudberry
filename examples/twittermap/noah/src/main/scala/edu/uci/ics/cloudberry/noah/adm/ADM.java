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
            Date dt = new Date(srcDateTimeFmt.parse(srcDateTimeString).getTime());
            StringBuilder sbDateTime = new StringBuilder();
            sbDateTime.append("datetime(\"").append(ADMDateFormat.format(dt)).append("T").append(ADMTimeFormat.format(dt)).append("\")");
            return sbDateTime.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public static String mkDateStringFromTweet(String srcDateTimeString) {
        try {
            Date dt = new Date(srcDateTimeFmt.parse(srcDateTimeString).getTime());
            StringBuilder sbDateTime = new StringBuilder();
            sbDateTime.append("date(\"").append(ADMDateFormat.format(dt)).append("\")");
            return sbDateTime.toString();
        } catch (Exception ex) {
            return null;
        }
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
