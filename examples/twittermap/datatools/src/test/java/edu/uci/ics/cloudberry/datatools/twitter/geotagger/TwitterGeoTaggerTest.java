package edu.uci.ics.cloudberry.datatools.twitter.geotagger;

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;

public class TwitterGeoTaggerTest {
    static String sampleTweet = "{\n" +
            "   \"created_at\":\"Mon Apr 01 15:53:45 +0000 2019\",\n" +
            "   \"id\":1112744870182031361,\n" +
            "   \"id_str\":\"1112744870182031361\",\n" +
            "   \"text\":\"Everyone from my old job keeps texting me they heard i left \\ud83d\\ude02\",\n" +
            "   \"source\":\"\\u003ca href=\\\"http:\\/\\/twitter.com\\/download\\/iphone\\\" rel=\\\"nofollow\\\"\\u003eTwitter for iPhone\\u003c\\/a\\u003e\",\n" +
            "   \"truncated\":false,\n" +
            "   \"in_reply_to_status_id\":null,\n" +
            "   \"in_reply_to_status_id_str\":null,\n" +
            "   \"in_reply_to_user_id\":null,\n" +
            "   \"in_reply_to_user_id_str\":null,\n" +
            "   \"in_reply_to_screen_name\":null,\n" +
            "   \"user\":{\n" +
            "      \"id\":186944989,\n" +
            "      \"id_str\":\"186944989\",\n" +
            "      \"name\":\"Sa$ha\",\n" +
            "      \"screen_name\":\"taylormone__\",\n" +
            "      \"location\":\"DC | BK \",\n" +
            "      \"url\":null,\n" +
            "      \"description\":\"If you take tweets personal, that\\u2019s on you \\u2728\",\n" +
            "      \"translator_type\":\"none\",\n" +
            "      \"protected\":false,\n" +
            "      \"verified\":false,\n" +
            "      \"followers_count\":1233,\n" +
            "      \"friends_count\":934,\n" +
            "      \"listed_count\":14,\n" +
            "      \"favourites_count\":18548,\n" +
            "      \"statuses_count\":70524,\n" +
            "      \"created_at\":\"Sat Sep 04 20:53:59 +0000 2010\",\n" +
            "      \"utc_offset\":null,\n" +
            "      \"time_zone\":null,\n" +
            "      \"geo_enabled\":true,\n" +
            "      \"lang\":\"en\",\n" +
            "      \"contributors_enabled\":false,\n" +
            "      \"is_translator\":false,\n" +
            "      \"profile_background_color\":\"669EFF\",\n" +
            "      \"profile_background_image_url\":\"http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme11\\/bg.gif\",\n" +
            "      \"profile_background_image_url_https\":\"https:\\/\\/abs.twimg.com\\/images\\/themes\\/theme11\\/bg.gif\",\n" +
            "      \"profile_background_tile\":true,\n" +
            "      \"profile_link_color\":\"F50C69\",\n" +
            "      \"profile_sidebar_border_color\":\"000000\",\n" +
            "      \"profile_sidebar_fill_color\":\"DAE0E3\",\n" +
            "      \"profile_text_color\":\"040D05\",\n" +
            "      \"profile_use_background_image\":true,\n" +
            "      \"profile_image_url\":\"http:\\/\\/pbs.twimg.com\\/profile_images\\/1101586425064112129\\/QrswgZu-_normal.jpg\",\n" +
            "      \"profile_image_url_https\":\"https:\\/\\/pbs.twimg.com\\/profile_images\\/1101586425064112129\\/QrswgZu-_normal.jpg\",\n" +
            "      \"profile_banner_url\":\"https:\\/\\/pbs.twimg.com\\/profile_banners\\/186944989\\/1516814036\",\n" +
            "      \"default_profile\":false,\n" +
            "      \"default_profile_image\":false,\n" +
            "      \"following\":null,\n" +
            "      \"follow_request_sent\":null,\n" +
            "      \"notifications\":null\n" +
            "   },\n" +
            "   \"geo\":null,\n" +
            "   \"coordinates\":null,\n" +
            "   \"place\":{\n" +
            "      \"id\":\"011add077f4d2da3\",\n" +
            "      \"url\":\"https:\\/\\/api.twitter.com\\/1.1\\/geo\\/id\\/011add077f4d2da3.json\",\n" +
            "      \"place_type\":\"city\",\n" +
            "      \"name\":\"Brooklyn\",\n" +
            "      \"full_name\":\"Brooklyn, NY\",\n" +
            "      \"country_code\":\"US\",\n" +
            "      \"country\":\"United States\",\n" +
            "      \"bounding_box\":{\n" +
            "         \"type\":\"Polygon\",\n" +
            "         \"coordinates\":[\n" +
            "            [\n" +
            "               [\n" +
            "                  -74.041878,\n" +
            "                  40.570842\n" +
            "               ],\n" +
            "               [\n" +
            "                  -74.041878,\n" +
            "                  40.739434\n" +
            "               ],\n" +
            "               [\n" +
            "                  -73.855673,\n" +
            "                  40.739434\n" +
            "               ],\n" +
            "               [\n" +
            "                  -73.855673,\n" +
            "                  40.570842\n" +
            "               ]\n" +
            "            ]\n" +
            "         ]\n" +
            "      },\n" +
            "      \"attributes\":{\n" +
            "\n" +
            "      }\n" +
            "   },\n" +
            "   \"contributors\":null,\n" +
            "   \"is_quote_status\":false,\n" +
            "   \"quote_count\":0,\n" +
            "   \"reply_count\":0,\n" +
            "   \"retweet_count\":0,\n" +
            "   \"favorite_count\":0,\n" +
            "   \"entities\":{\n" +
            "      \"hashtags\":[\n" +
            "\n" +
            "      ],\n" +
            "      \"urls\":[\n" +
            "\n" +
            "      ],\n" +
            "      \"user_mentions\":[\n" +
            "\n" +
            "      ],\n" +
            "      \"symbols\":[\n" +
            "\n" +
            "      ]\n" +
            "   },\n" +
            "   \"favorited\":false,\n" +
            "   \"retweeted\":false,\n" +
            "   \"filter_level\":\"low\",\n" +
            "   \"lang\":\"en\",\n" +
            "   \"timestamp_ms\":\"1554134025508\"\n" +
            "}";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        USGeoGnosis usGeoGnosis = USGeoGnosisLoader.loadUSGeoGnosis("web/public/data/state.json",
                "web/public/data/county.json",
                "web/public/data/city.json");
        long end = System.currentTimeMillis();
        System.out.println("Initializing USGeoGnosis takes time: " + (end - start) / 1000.0 + " seconds.");

        TwitterGeoTagger.DEBUG = true;

        TwitterGeoTagger.printTagOneTweet(usGeoGnosis, sampleTweet);
    }
}
