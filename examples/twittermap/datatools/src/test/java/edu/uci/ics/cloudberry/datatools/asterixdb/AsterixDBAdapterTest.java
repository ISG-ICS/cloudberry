package edu.uci.ics.cloudberry.datatools.asterixdb;

import edu.uci.ics.cloudberry.datatools.twitter.geotagger.TwitterGeoTagger;
import java.util.Map;

public class AsterixDBAdapterTest {

    String sampleTweet = "{\n" +
            "   \"created_at\":\"Mon Apr 01 15:53:45 +0000 2019\",\n" +
            "   \"id\":1112744871062839297,\n" +
            "   \"id_str\":\"1112744871062839297\",\n" +
            "   \"text\":\"Just think I got pranked by the post office? Received my April Visa statement from 2018 \\ud83e\\udd26\\u200d\\u2640\\ufe0f\\ud83e\\udd74 #seriously\\u2026 https:\\/\\/t.co\\/HDT3kzgmr1\",\n" +
            "   \"source\":\"\\u003ca href=\\\"http:\\/\\/instagram.com\\\" rel=\\\"nofollow\\\"\\u003eInstagram\\u003c\\/a\\u003e\",\n" +
            "   \"truncated\":true,\n" +
            "   \"in_reply_to_status_id\":null,\n" +
            "   \"in_reply_to_status_id_str\":null,\n" +
            "   \"in_reply_to_user_id\":null,\n" +
            "   \"in_reply_to_user_id_str\":null,\n" +
            "   \"in_reply_to_screen_name\":null,\n" +
            "   \"user\":{\n" +
            "      \"id\":26009081,\n" +
            "      \"id_str\":\"26009081\",\n" +
            "      \"name\":\"Sarah Picot-Kirkby\",\n" +
            "      \"screen_name\":\"noshoesgal\",\n" +
            "      \"location\":\"Bahamas\",\n" +
            "      \"url\":\"http:\\/\\/barefootmarketing.net\\/\",\n" +
            "      \"description\":\"Owner of Barefoot Marketing , Barefoot Locations and 242newsbahamas in The Bahamas.\",\n" +
            "      \"translator_type\":\"none\",\n" +
            "      \"protected\":false,\n" +
            "      \"verified\":false,\n" +
            "      \"followers_count\":742,\n" +
            "      \"friends_count\":982,\n" +
            "      \"listed_count\":10,\n" +
            "      \"favourites_count\":127,\n" +
            "      \"statuses_count\":1171,\n" +
            "      \"created_at\":\"Mon Mar 23 14:22:06 +0000 2009\",\n" +
            "      \"utc_offset\":null,\n" +
            "      \"time_zone\":null,\n" +
            "      \"geo_enabled\":true,\n" +
            "      \"lang\":\"en\",\n" +
            "      \"contributors_enabled\":false,\n" +
            "      \"is_translator\":false,\n" +
            "      \"profile_background_color\":\"EDECE9\",\n" +
            "      \"profile_background_image_url\":\"http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme3\\/bg.gif\",\n" +
            "      \"profile_background_image_url_https\":\"https:\\/\\/abs.twimg.com\\/images\\/themes\\/theme3\\/bg.gif\",\n" +
            "      \"profile_background_tile\":false,\n" +
            "      \"profile_link_color\":\"94D487\",\n" +
            "      \"profile_sidebar_border_color\":\"FFFFFF\",\n" +
            "      \"profile_sidebar_fill_color\":\"E3E2DE\",\n" +
            "      \"profile_text_color\":\"634047\",\n" +
            "      \"profile_use_background_image\":true,\n" +
            "      \"profile_image_url\":\"http:\\/\\/pbs.twimg.com\\/profile_images\\/1065629366791487488\\/I5pRkqrJ_normal.jpg\",\n" +
            "      \"profile_image_url_https\":\"https:\\/\\/pbs.twimg.com\\/profile_images\\/1065629366791487488\\/I5pRkqrJ_normal.jpg\",\n" +
            "      \"profile_banner_url\":\"https:\\/\\/pbs.twimg.com\\/profile_banners\\/26009081\\/1538977688\",\n" +
            "      \"default_profile\":false,\n" +
            "      \"default_profile_image\":false,\n" +
            "      \"following\":null,\n" +
            "      \"follow_request_sent\":null,\n" +
            "      \"notifications\":null\n" +
            "   },\n" +
            "   \"geo\":{\n" +
            "      \"type\":\"Point\",\n" +
            "      \"coordinates\":[\n" +
            "         26.5356,\n" +
            "         -78.69696\n" +
            "      ]\n" +
            "   },\n" +
            "   \"coordinates\":{\n" +
            "      \"type\":\"Point\",\n" +
            "      \"coordinates\":[\n" +
            "         -78.69696,\n" +
            "         26.5356\n" +
            "      ]\n" +
            "   },\n" +
            "   \"place\":{\n" +
            "      \"id\":\"b631437cf2f16804\",\n" +
            "      \"url\":\"https:\\/\\/api.twitter.com\\/1.1\\/geo\\/id\\/b631437cf2f16804.json\",\n" +
            "      \"place_type\":\"country\",\n" +
            "      \"name\":\"Bahamas\",\n" +
            "      \"full_name\":\"Bahamas\",\n" +
            "      \"country_code\":\"BS\",\n" +
            "      \"country\":\"Bahamas\",\n" +
            "      \"bounding_box\":{\n" +
            "         \"type\":\"Polygon\",\n" +
            "         \"coordinates\":[\n" +
            "            [\n" +
            "               [\n" +
            "                  -80.475610,\n" +
            "                  20.912263\n" +
            "               ],\n" +
            "               [\n" +
            "                  -80.475610,\n" +
            "                  27.237671\n" +
            "               ],\n" +
            "               [\n" +
            "                  -72.712276,\n" +
            "                  27.237671\n" +
            "               ],\n" +
            "               [\n" +
            "                  -72.712276,\n" +
            "                  20.912263\n" +
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
            "   \"extended_tweet\":{\n" +
            "      \"full_text\":\"Just think I got pranked by the post office? Received my April Visa statement from 2018 \\ud83e\\udd26\\u200d\\u2640\\ufe0f\\ud83e\\udd74 #seriously #mailaintbetterinthebahamas #muddasick\\u2026 https:\\/\\/t.co\\/g9F9XytTuB\",\n" +
            "      \"display_text_range\":[\n" +
            "         0,\n" +
            "         168\n" +
            "      ],\n" +
            "      \"entities\":{\n" +
            "         \"hashtags\":[\n" +
            "            {\n" +
            "               \"text\":\"seriously\",\n" +
            "               \"indices\":[\n" +
            "                  94,\n" +
            "                  104\n" +
            "               ]\n" +
            "            },\n" +
            "            {\n" +
            "               \"text\":\"mailaintbetterinthebahamas\",\n" +
            "               \"indices\":[\n" +
            "                  105,\n" +
            "                  132\n" +
            "               ]\n" +
            "            },\n" +
            "            {\n" +
            "               \"text\":\"muddasick\",\n" +
            "               \"indices\":[\n" +
            "                  133,\n" +
            "                  143\n" +
            "               ]\n" +
            "            }\n" +
            "         ],\n" +
            "         \"urls\":[\n" +
            "            {\n" +
            "               \"url\":\"https:\\/\\/t.co\\/g9F9XytTuB\",\n" +
            "               \"expanded_url\":\"https:\\/\\/www.instagram.com\\/p\\/Bvt_pGgHT_sRFxoRsW0RexBMkLSip4yBQvS6gI0\\/?utm_source=ig_twitter_share&igshid=1emy4yd6uomwx\",\n" +
            "               \"display_url\":\"instagram.com\\/p\\/Bvt_pGgHT_sR\\u2026\",\n" +
            "               \"indices\":[\n" +
            "                  145,\n" +
            "                  168\n" +
            "               ]\n" +
            "            }\n" +
            "         ],\n" +
            "         \"user_mentions\":[\n" +
            "\n" +
            "         ],\n" +
            "         \"symbols\":[\n" +
            "\n" +
            "         ]\n" +
            "      }\n" +
            "   },\n" +
            "   \"quote_count\":0,\n" +
            "   \"reply_count\":0,\n" +
            "   \"retweet_count\":0,\n" +
            "   \"favorite_count\":0,\n" +
            "   \"entities\":{\n" +
            "      \"hashtags\":[\n" +
            "         {\n" +
            "            \"text\":\"seriously\",\n" +
            "            \"indices\":[\n" +
            "               94,\n" +
            "               104\n" +
            "            ]\n" +
            "         }\n" +
            "      ],\n" +
            "      \"urls\":[\n" +
            "         {\n" +
            "            \"url\":\"https:\\/\\/t.co\\/HDT3kzgmr1\",\n" +
            "            \"expanded_url\":\"https:\\/\\/twitter.com\\/i\\/web\\/status\\/1112744871062839297\",\n" +
            "            \"display_url\":\"twitter.com\\/i\\/web\\/status\\/1\\u2026\",\n" +
            "            \"indices\":[\n" +
            "               106,\n" +
            "               129\n" +
            "            ]\n" +
            "         }\n" +
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
            "   \"possibly_sensitive\":false,\n" +
            "   \"filter_level\":\"low\",\n" +
            "   \"lang\":\"en\",\n" +
            "   \"timestamp_ms\":\"1554134025718\"\n" +
            "}";

    public boolean testGeneralTwitter() throws Exception {
        Map<String, Object> tweetObject = TwitterGeoTagger.parseOneTweet(sampleTweet);
        AsterixDBAdapterForGeneralTwitter asterixDBAdapterForGeneralTwitter = new AsterixDBAdapterForGeneralTwitter();
        String asterixDBTuple = asterixDBAdapterForGeneralTwitter.transform(tweetObject);
        System.out.println(asterixDBTuple);
        return true;
    }

    public boolean testTwitter() throws Exception {
        Map<String, Object> tweetObject = TwitterGeoTagger.parseOneTweet(sampleTweet);
        AsterixDBAdapterForTwitter asterixDBAdapterForTwitter = new AsterixDBAdapterForTwitter();
        String asterixDBTuple = asterixDBAdapterForTwitter.transform(tweetObject);
        System.out.println(asterixDBTuple);
        return true;
    }

    public static void main(String[] args) {
        AsterixDBAdapterTest test = new AsterixDBAdapterTest();
        try {
            test.testGeneralTwitter();
            test.testTwitter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
