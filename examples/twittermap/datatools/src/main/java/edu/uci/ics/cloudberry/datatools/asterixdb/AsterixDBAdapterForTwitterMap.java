package edu.uci.ics.cloudberry.datatools.asterixdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.cloudberry.util.Rectangle;
import org.apache.commons.lang3.StringEscapeUtils;
import java.util.*;

import static edu.uci.ics.cloudberry.datatools.twitter.geotagger.TwitterGeoTagger.boundingBox2Rectangle;

/**
 * AsterixDBAdapterForTwitterMap
 *
 *  - Implementation of AsterixDBAdapter for TwitterMap application data
 *
 *  TODO - re-implement this by using the old way of Tweet.toADM (object hard-coded transform)
 *
 * @author Qiushi Bai
 */
public class AsterixDBAdapterForTwitterMap implements AsterixDBAdapter {

    public String POINT = "point";
    public String RECTANGLE = "rectangle";

    // Map<String, String> - <columnName, dataType>
    public Map<String, Object> schema;
    public Map<String, String> stringSetKeys;
    public Map<String, String> rename;

    public AsterixDBAdapterForTwitterMap() {

        // Twitter uses UTC timezone
        tweetDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // currently hard coded with TwitterMap schema
        // TODO - read the schema from a config file
        schema = new HashMap<>();
        stringSetKeys = new HashMap<>();
        rename = new HashMap<>();
        schema.put("created_at", DATETIME);
        rename.put("created_at", "create_at");
        schema.put("id", INT64);
        schema.put("text", STRING);
        schema.put("in_reply_to_status_id", INT64);
        rename.put("in_reply_to_status_id", "in_reply_to_status");
        schema.put("in_reply_to_user_id", INT64);
        rename.put("in_reply_to_user_id", "in_reply_to_user");
        schema.put("favorite_count", VALUE);
        schema.put("retweet_count", VALUE);
        schema.put("lang", STRING);
        schema.put("is_retweet", VALUE);
        schema.put("coordinates", POINT);
        rename.put("coordinates", "coordinate");
        schema.put("hashtags", STRING_SET);
        stringSetKeys.put("hashtags", "text");
        schema.put("user_mentions", STRING_SET);
        stringSetKeys.put("user_mentions", "id");

        Map<String, String> place = new HashMap<>();
        place.put("country", STRING);
        place.put("country_code", STRING);
        place.put("full_name", STRING);
        place.put("id", STRING);
        place.put("name", STRING);
        place.put("place_type", STRING);
        place.put("bounding_box", RECTANGLE);
        schema.put("place", place);

        Map<String, String> user = new HashMap<>();
        user.put("id", INT64);
        user.put("name", STRING);
        user.put("screen_name", STRING);
        user.put("profile_image_url", STRING);
        user.put("lang", STRING);
        user.put("location", STRING);
        user.put("created_at", DATE);
        user.put("description", STRING);
        user.put("followers_count", VALUE);
        user.put("friends_count", VALUE);
        user.put("statues_count", VALUE);
        schema.put("user", user);

        Map<String, String> geoTag = new HashMap<>();
        geoTag.put("stateID", VALUE);
        geoTag.put("stateName", STRING);
        geoTag.put("countyID", VALUE);
        geoTag.put("countyName", STRING);
        geoTag.put("cityID", VALUE);
        geoTag.put("cityName", STRING);
        schema.put("geo_tag", geoTag);
    }

    public String transform(String tweet) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> tuple = mapper.readValue(tweet, Map.class);
        return transform(tuple);
    }

    public String transform(Map<String, Object> tuple) throws Exception {

        /** Hard coded special treatment for Twitter data
         *   - if twitter is 'truncated',
         *       use the 'extended_tweet'->'full_text' to replace 'text'
         * */
        if (tuple.containsKey("truncated") && (Boolean)tuple.get("truncated")) {
            if (tuple.containsKey("extended_tweet")) {
                Map<String, Object> extendedTweet = (Map<String, Object>) tuple.get("extended_tweet");
                if (extendedTweet.containsKey("full_text")) {
                    tuple.put("text", extendedTweet.get("full_text"));
                }
            }
        }

        return transformObject(tuple, schema);
    }

    public String transformObject(Map<String, Object> object, Map<String, Object> schema) throws Exception {
        if (object == null) return "null";
        StringBuilder tuple = new StringBuilder();
        tuple.append("{");
        int i = 0;
        // recursively transform object to target format String
        for (Map.Entry<String, Object> entry: object.entrySet()) {
            if (schema.containsKey(entry.getKey())) {
                // recursive schema
                if (schema.get(entry.getKey()) instanceof Map) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> subSchema = (Map) schema.get(entry.getKey());
                        if (i >= 1) {
                            tuple.append(",");
                        }
                        transformKey(tuple, entry.getKey());
                        tuple.append(transformObject((Map<String, Object>) entry.getValue(), subSchema));
                        i ++;
                    }
                    // object for this key is null
                    else if (entry.getValue() == null) {
                        if (i >= 1) {
                            tuple.append(",");
                        }
                        transformKey(tuple, entry.getKey());
                        tuple.append("null");
                        i ++;
                    }
                    else {
                        System.err.println("[AsterixDBAdapter] tuple does not match schema!");
                        //-DEBUG-//
                        System.err.println("key = " + entry.getKey());
                        System.err.println("value = " + entry.getValue());
                        return "";
                    }
                }
                // flat schema
                else {
                    if (i >= 1) {
                        tuple.append(",");
                    }
                    transformKey(tuple, entry.getKey());
                    transformColumn(tuple, entry.getKey(), (String) schema.get(entry.getKey()), entry.getValue());
                    i ++;
                }
            }
        }
        tuple.append("}");
        return tuple.toString().replaceAll("\\s+", " ");
    }

    public void transformKey(StringBuilder tuple, String key) {
        if (rename.containsKey(key)) {
            tuple.append('"').append(StringEscapeUtils.escapeJava(rename.get(key))).append('"');
        }
        else {
            tuple.append('"').append(StringEscapeUtils.escapeJava(key)).append('"');
        }
        tuple.append(":");
    }

    public void transformColumn(StringBuilder tuple, String key, String type, Object value) throws Exception {
        if (value == null) {
            tuple.append("null");
            return;
        }
        switch (type) {
            case "date":
                Date date = getDate((String) value);
                tuple.append("date(\"" + dateFormat.format(date) + "\")");
                break;
            case "datetime":
                date = AsterixDBAdapter.getDate((String) value);
                tuple.append("datetime(\"" + dateFormat.format(date) + "T" + timeFormat.format(date) + "\")");
                break;
            case "int64":
                tuple.append( "int64(\"" + value + "\")");
                break;
            case "string":
                tuple.append('"').append(StringEscapeUtils.escapeJava((String)value).replaceAll("\\s+", " ")).append('"');
                break;
            case "value":
                tuple.append(value);
                break;
            case "string_set":
                List<String> stringSets = (List<String>) value;
                if (stringSets == null) {
                    tuple.append("null");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < stringSets.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append('"').append(StringEscapeUtils.escapeJava(stringSets.get(i)).replaceAll("\\s+", " ")).append('"');
                }
                sb.append("]");
                tuple.append(sb.toString());
                break;
            case "value_set":
                List<Object> valueSets = (List<Object>) value;
                if (valueSets == null) {
                    tuple.append("null");
                    return;
                }
                sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < valueSets.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(StringEscapeUtils.escapeJava(String.valueOf(valueSets.get(i))));
                }
                sb.append("]");
                tuple.append(sb.toString());
                break;
            case "point":
                Map<String, Object> coordinates = (Map<String, Object> ) value;
                if (coordinates == null) {
                    tuple.append("null");
                    return;
                }
                List<Double> coordinate = (List<Double>) coordinates.get("coordinates");
                if (coordinate == null || coordinate.size() != 2) {
                    tuple.append("null");
                    return;
                }
                tuple.append("point(\"" + coordinate.get(0) + "," + coordinate.get(1) + "\")");
                break;
            case "rectangle":
                Map<String, Object> boundingBox = (Map<String, Object>) value;
                if (boundingBox == null) {
                    tuple.append("null");
                    return;
                }
                // TODO - this logic was migrated from legacy code, should be revisited for newer version Twitter APIs.
                List<List<List<Double>>> bbCoordinates = (List<List<List<Double>>>) boundingBox.get("coordinates");
                if (bbCoordinates == null || bbCoordinates.size() != 1 || (bbCoordinates.get(0).size() != 4 && bbCoordinates.get(0).size() != 2)) {
                    tuple.append("null");
                    return;
                }
                sb = new StringBuilder("rectangle");
                Rectangle rectangle = boundingBox2Rectangle(bbCoordinates.get(0));
                sb.append("(\"").append(rectangle.swLog()).append(',')
                        .append(rectangle.swLat())
                        .append(' ')
                        .append(rectangle.neLog()).append(',')
                        .append(rectangle.neLat())
                        .append("\")");
                tuple.append(sb.toString());
                break;
            default:
                throw new Exception("unknown data type");
        }
    }

    public static Date getDate(String dateString) throws Exception {
        return tweetDateFormat.parse(dateString);
    }
}
