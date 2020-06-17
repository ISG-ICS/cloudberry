package edu.uci.ics.cloudberry.datatools.twitter.geotagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TwitterGeoTaggerTest {

    USGeoGnosis usGeoGnosis;

    String sampleTweet = "{ " +
                "\"create_at\": \"2020-06-11T05:20:14.000Z\", " +
                "\"id\": 1270948937063677952, " +
                "\"text\": \"6ix9ine \uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02 @ Galveston, Texas https://t.co/6cFB0H6r3k\", " +
                "\"in_reply_to_status\": -1, " +
                "\"in_reply_to_user\": -1, " +
                "\"favorite_count\": 0, " +
                "\"coordinate\": [-94.7942, 29.2995], " +
                "\"retweet_count\": 0, " +
                "\"lang\": \"lt\", " +
                "\"is_retweet\": false, " +
                "\"user\": { " +
                    "\"id\": 1115310534, " +
                    "\"name\": \"Silky Ash\", " +
                    "\"screen_name\": \"chopb4l\", " +
                    "\"profile_image_url\": \"http://pbs.twimg.com/profile_images/846881801041784832/O07bM0xD_normal.jpg\", " +
                    "\"lang\": \"null\", " +
                    "\"location\": \"New Orleans, LA\", " +
                    "\"create_at\": \"2013-01-23\", " +
                    "\"description\": \"Uptown 4 Life 12th Ward Soulja #WHODAT #BULLS #PELICANS #DAUNIT #RLLNR R.I.P. Nettra, R.I.P. Uncle Calvin, Grandma Mildred, R.I.P. Auntie Karen\", " +
                    "\"followers_count\": 1396, " +
                    "\"friends_count\": 4974, " +
                    "\"statues_count\": 14472 " +
                "}, " +
                "\"place\": { " +
                    "\"country\": \"United States\", " +
                    "\"country_code\": \"United States\", " +
                    "\"full_name\": \"Galveston, TX\", " +
                    "\"id\": \"632eeebc87aecd57\", " +
                    "\"name\": \"Galveston\", " +
                    "\"place_type\": \"city\", " +
                    "\"bounding_box\": [ [-94.880809, 29.239602], [-94.764742, 29.335548] ] " +
                "}" +
            "}";

    public TwitterGeoTaggerTest() {
        long start = System.currentTimeMillis();
        usGeoGnosis = USGeoGnosisLoader.loadUSGeoGnosis("web/public/data/state.json",
                "web/public/data/county.json",
                "web/public/data/city.json");
        long end = System.currentTimeMillis();
        System.out.println("Initializing USGeoGnosis takes time: " + (end - start) / 1000.0 + " seconds.");
    }

    public boolean testParseOneTweet() {
        Map<String, Object> tweetObject;
        try {
            tweetObject = TwitterGeoTagger.parseOneTweet(sampleTweet);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Map<String, Object> place = (Map<String, Object>) tweetObject.get("place");
        System.out.println("place: {");
        System.out.println("    country: " + place.get("country"));
        System.out.println("    country_code: " + place.get("country_code"));
        System.out.println("    full_name: " + place.get("full_name"));
        System.out.println("    id: " + place.get("id"));
        System.out.println("    name: " + place.get("name"));
        System.out.println("    place_type: " + place.get("place_type"));
        System.out.println("    bounding_box: [");
        List<List<Double>> boundingBox = (List<List<Double>>) place.get("bounding_box");
        for (List<Double> point: boundingBox) {
            System.out.println("        [" + point.get(0) + ", " + point.get(1) + "]");
        }
        System.out.println("    ]");

        return true;
    }

    public boolean testTextMatchPlace() throws IOException {
        Map<String, Object>  tweetObject = TwitterGeoTagger.parseOneTweet(sampleTweet);
        boolean success = TwitterGeoTagger.textMatchPlace(usGeoGnosis, tweetObject);

        ObjectMapper mapper = new ObjectMapper();
        String tweet = mapper.writeValueAsString(tweetObject);
        System.out.println("==== After text match place ====");
        System.out.println(tweet);

        return success;
    }

    public static void main(String[] args) throws IOException {

        TwitterGeoTaggerTest test = new TwitterGeoTaggerTest();

        test.testParseOneTweet();

        test.testTextMatchPlace();
    }
}
