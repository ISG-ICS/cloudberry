package edu.uci.ics.cloudberry.datatools.asterixdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * AsterixDBAdapterForTwitter
 *
 *  - Implementation of AsterixDBAdapter for general Twitter data
 *
 *  TODO - This does not work for now,
 *         because the mapper.writeValueAsString() will quote `datetime` function call in the output String
 *
 *  @author Qiushi Bai
 */
public class AsterixDBAdapterForGeneralTwitter implements AsterixDBAdapter {

    public AsterixDBAdapterForGeneralTwitter() {

        // Twitter uses UTC timezone
        tweetDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public String transform(String tweet) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> tuple = mapper.readValue(tweet, Map.class);
        return transform(tuple);
    }

    public String transform(Map<String, Object> tuple) throws Exception {

        /**
         * (1) Make sure "text" is always Non-truncated.
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

        /**
         * (2) Transform all 'created_at' attributes to be 'datetime' recursively
         * */
        transformCreatedAt(tuple);

        // write back to String
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(tuple);
    }

    public void transformCreatedAt(Map<String, Object> object) throws Exception {
        if (object == null) return;

        // traverse all attributes of object
        for (Map.Entry<String, Object> entry: object.entrySet()) {
            // if attribute is an object
            if (entry.getValue() instanceof Map) {
                // recursive call
                transformCreatedAt((Map<String, Object>)entry.getValue());
            }
            // else attribute is flat
            else {
                // if this attribute is called 'created_at'
                if (entry.getKey().equalsIgnoreCase("created_at")) {
                    Date date = AsterixDBAdapter.getDate((String) entry.getValue());
                    entry.setValue("datetime(\"" + dateFormat.format(date) + "T" + timeFormat.format(date) + "\")");
                }
            }
        }
    }
}
