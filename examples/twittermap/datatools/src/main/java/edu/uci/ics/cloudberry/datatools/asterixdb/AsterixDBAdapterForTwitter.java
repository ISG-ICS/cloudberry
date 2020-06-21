package edu.uci.ics.cloudberry.datatools.asterixdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import java.util.*;

/**
 * AsterixDBAdapterForTwitter
 *
 *  - Implementation of AsterixDBAdapter for general Twitter data
 *
 *  @author Qiushi Bai
 */
public class AsterixDBAdapterForTwitter implements AsterixDBAdapter {

    // Map<String, String> - <columnName, dataType>
    public Map<String, Object> schema;

    public AsterixDBAdapterForTwitter() {

        // Twitter uses UTC timezone
        tweetDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        /**
         * Twitter JSON schema
         *   https://developer.twitter.com/en/docs/tweets/data-dictionary/overview/tweet-object
         * */
        // Tweet Object
        schema = new HashMap<>();
        schema.put("created_at", DATETIME);
        schema.put("id", INT64);
        schema.put("id_str", STRING);
        schema.put("text", STRING);
        schema.put("source", STRING);
        schema.put("truncated", VALUE);
        schema.put("in_reply_to_status_id", INT64);
        schema.put("in_reply_to_status_id_str", STRING);
        schema.put("in_reply_to_user_id", INT64);
        schema.put("in_reply_to_user_id_str", STRING);
        schema.put("in_reply_to_screen_name", STRING);

        // User Object
        // https://developer.twitter.com/en/docs/tweets/data-dictionary/overview/user-object
        Map<String, String> user = new HashMap<>();
        user.put("id", INT64);
        user.put("id_str", STRING);
        user.put("name", STRING);
        user.put("screen_name", STRING);
        user.put("location", STRING);
        // skip "derived" - enterprise API only
        user.put("url", STRING);
        user.put("description", STRING);
        user.put("protected", VALUE);
        user.put("verified", VALUE);
        user.put("followers_count", VALUE);
        user.put("friends_count", VALUE);
        user.put("listed_count", VALUE);
        user.put("favourites_count", VALUE);
        user.put("statues_count", VALUE);
        user.put("created_at", DATETIME);
        user.put("profile_banner_url", STRING);
        user.put("profile_image_url_https", STRING);
        user.put("default_profile", VALUE);
        user.put("default_profile_image", VALUE);
        user.put("withheld_in_countries", STRING_SET);
        user.put("withheld_scope", STRING);
        schema.put("user", user);

        // Coordinates Object
        // https://developer.twitter.com/en/docs/tweets/data-dictionary/overview/geo-objects#coordinates-dictionary
        Map<String, String> coordinates = new HashMap<>();
        coordinates.put("coordinates", VALUE_SET);
        coordinates.put("type", STRING);
        schema.put("coordinates", coordinates);

        // Place Object
        // https://developer.twitter.com/en/docs/tweets/data-dictionary/overview/geo-objects#place-dictionary
        Map<String, String> place = new HashMap<>();
        place.put("id", STRING);
        place.put("url", STRING);
        place.put("place_type", STRING);
        place.put("name", STRING);
        place.put("full_name", STRING);
        place.put("country_code", STRING);
        place.put("country", STRING);
        place.put("bounding_box", BOUNDING_BOX);
        schema.put("place", place);

        schema.put("quoted_status_id", INT64);
        schema.put("quoted_status_id_str", STRING);
        schema.put("is_quote_status", VALUE);

        // quote_status Tweet Object
        // TODO

        // retweeted_status Tweet Object
        // TODO

        schema.put("quote_count", VALUE);
        schema.put("reply_count", VALUE);
        schema.put("retweet_count", VALUE);
        schema.put("favorite_count", VALUE);

        // entities
        // https://developer.twitter.com/en/docs/tweets/data-dictionary/overview/entities-object
        // TODO

        // extended_entities
        // TODO

        schema.put("favorited", VALUE);
        schema.put("retweeted", VALUE);
        schema.put("possibly_sensitive", VALUE);
        schema.put("filter_level", STRING);
        schema.put("lang", STRING);
        // skip matching_rules

        // additional columns added by our own TwitterGeoTagger
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
        tuple.append('"').append(StringEscapeUtils.escapeJava(key)).append('"');
        tuple.append(":");
    }

    public void transformColumn(StringBuilder tuple, String key, String type, Object value) throws Exception {
        if (value == null) {
            tuple.append("null");
            return;
        }
        switch (type) {
            case "datetime":
                Date date = AsterixDBAdapter.getDate((String) value);
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
            case "bounding_box":
                Map<String, Object> boundingBox = (Map<String, Object>) value;
                if (boundingBox == null) {
                    tuple.append("null");
                    return;
                }
                List<List<List<Double>>> bbCoordinates = (List<List<List<Double>>>) boundingBox.get("coordinates");
                sb = new StringBuilder();
                sb.append("{");
                sb.append('"').append("coordinates").append('"').append(":");
                sb.append("[[");
                for (int i = 0; i < bbCoordinates.get(0).size(); i ++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    List<Double> coordinate = bbCoordinates.get(0).get(i);
                    sb.append("[").append(coordinate.get(0)).append(",").append(coordinate.get(1)).append("]");
                }
                sb.append("]]");
                sb.append(",");
                sb.append('"').append("type").append('"').append(":");
                sb.append('"').append(boundingBox.get("type")).append('"');
                sb.append("}");
                tuple.append(sb.toString());
                break;
            default:
                System.err.println("key = " + key + ", type = " + type + ", value = " + value);
                throw new Exception("unknown data type");
        }
    }
}
