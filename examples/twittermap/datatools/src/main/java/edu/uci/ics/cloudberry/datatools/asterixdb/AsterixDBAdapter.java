package edu.uci.ics.cloudberry.datatools.asterixdb;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * AsterixDBAdapter
 *
 *  - provides APIs to transform a JSON record to AsterixDB format (currently ADM)
 *
 * @author Qiushi Bai
 */
public interface AsterixDBAdapter {

    SimpleDateFormat tweetDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSSZZZZ");

    String DATETIME = "datetime";
    String INT64 = "int64";
    String STRING = "string"; // quoted value (suitable for string)
    String VALUE = "value"; // no quoted value (suitable for int, boolean types)
    String STRING_SET = "string_set"; // list of quoted value
    String VALUE_SET = "value_set"; // list of no quoted value
    String BOUNDING_BOX = "bounding_box"; // special treatment to bounding_box column

    String transform(String tweet) throws Exception;

    String transform(Map<String, Object> tuple) throws Exception;

    static Date getDate(String dateString) throws Exception {
        return tweetDateFormat.parse(dateString);
    }
}
