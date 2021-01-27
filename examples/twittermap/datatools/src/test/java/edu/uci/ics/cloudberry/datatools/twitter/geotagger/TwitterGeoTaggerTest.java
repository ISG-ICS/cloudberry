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

    static String sampleInvalidTweet = "{" +
            "'created_at': 'Mon Aug 10 06:59:58 +0000 2020', " +
            "'id': 1292717307618381825, " +
            "'id_str': '1292717307618381825', " +
            "'text': '@doreendibona @DrGJackBrown @marcorubio @marcorubio You’re in NO position to talk. COVID-19’s made Florida its home… https://t.co/e81q6OKLx9', " +
            "'display_text_range': [40, 140], " +
            "'source': '<a href=\"http://twitter.com/download/iphone\" rel=\"nofollow\">Twitter for iPhone</a>', " +
            "'truncated': True, " +
            "'in_reply_to_status_id': 1292643311627493380, " +
            "'in_reply_to_status_id_str': '1292643311627493380', " +
            "'in_reply_to_user_id': 738145794, " +
            "'in_reply_to_user_id_str': '738145794', " +
            "'in_reply_to_screen_name': 'doreendibona', " +
            "'user': {" +
            "    'id': 704925673954140161, " +
            "    'id_str': '704925673954140161', " +
            "    'name': 'Nilsa', " +
            "    'screen_name': 'Nilsalollitasmo', " +
            "    'location': 'New York, USA', " +
            "    'url': None, " +
            "    'description': \"Passionate Boricua dog mom, SYFY Gk, DC&Marvel, ❤️'s Outlander, Last Kingdom, Wizard of Oz, Arts, ❤️fearless pple, Travel, Animals, Blu wave \uD83C\uDF0A, Proud RESISTER\", " +
            "    'translator_type': 'none', " +
            "    'protected': False, " +
            "    'verified': False, " +
            "    'followers_count': 1445, " +
            "    'friends_count': 4794, " +
            "    'listed_count': 3, " +
            "    'favourites_count': 13490, " +
            "    'statuses_count': 21025, " +
            "    'created_at': 'Wed Mar 02 07:05:52 +0000 2016', " +
            "    'utc_offset': None, " +
            "    'time_zone': None, " +
            "    'geo_enabled': True, " +
            "    'lang': None, " +
            "    'contributors_enabled': False, " +
            "    'is_translator': False, " +
            "    'profile_background_color': 'F5F8FA', " +
            "    'profile_background_image_url': '', " +
            "    'profile_background_image_url_https': '', " +
            "    'profile_background_tile': False, " +
            "    'profile_link_color': '1DA1F2', " +
            "    'profile_sidebar_border_color': 'C0DEED', " +
            "    'profile_sidebar_fill_color': 'DDEEF6', " +
            "    'profile_text_color': '333333', " +
            "    'profile_use_background_image': True, " +
            "    'profile_image_url': 'http://pbs.twimg.com/profile_images/1104914291168370688/nS0DZeu5_normal.jpg', " +
            "    'profile_image_url_https': 'https://pbs.twimg.com/profile_images/1104914291168370688/nS0DZeu5_normal.jpg', " +
            "    'profile_banner_url': 'https://pbs.twimg.com/profile_banners/704925673954140161/1596775904', " +
            "    'default_profile': True, " +
            "    'default_profile_image': False, " +
            "    'following': None, " +
            "    'follow_request_sent': None, " +
            "    'notifications': None" +
            "}, " +
            "'geo': None, " +
            "'coordinates': None, " +
            "'place': {" +
            "    'id': '94965b2c45386f87', " +
            "    'url': 'https://api.twitter.com/1.1/geo/id/94965b2c45386f87.json', " +
            "    'place_type': 'admin', " +
            "    'name': 'New York', " +
            "    'full_name': 'New York, USA', " +
            "    'country_code': 'US', " +
            "    'country': 'United States', " +
            "    'bounding_box': {" +
            "        'type': 'Polygon', " +
            "        'coordinates': [[[-79.76259, 40.477383], [-79.76259, 45.015851], [-71.777492, 45.015851], [-71.777492, 40.477383]]]" +
            "    }, " +
            "    'attributes': {}" +
            "}, " +
            "'contributors': None, " +
            "'is_quote_status': False, " +
            "'extended_tweet': {" +
            "    'full_text': '@doreendibona @DrGJackBrown @marcorubio @marcorubio You’re in NO position to talk. COVID-19’s made Florida its home b/c  politicians like you are kneeling to 45. Sadly, your constituents are paying 4 your sins. But it won’t be 4long. There’s a BLUE TITLEWAVE coming. It’s going to wipe out the @GOP &amp; @realDonaldTrump https://t.co/odX0Q5FYjo', " +
            "    'display_text_range': [40, 321], " +
            "    'entities': {" +
            "        'hashtags': [], " +
            "        'urls': [], " +
            "        'user_mentions': [" +
            "            {'screen_name': 'doreendibona', 'name': 'Doreen Di Bona', 'id': 738145794, 'id_str': '738145794', 'indices': [0, 13]}, " +
            "            {'screen_name': 'DrGJackBrown', 'name': 'Dr. Jack Brown', 'id': 212445456, 'id_str': '212445456', 'indices': [14, 27]}, " +
            "            {'screen_name': 'marcorubio', 'name': 'Marco Rubio', 'id': 15745368, 'id_str': '15745368', 'indices': [28, 39]}, " +
            "            {'screen_name': 'marcorubio', 'name': 'Marco Rubio', 'id': 15745368, 'id_str': '15745368', 'indices': [40, 51]}, " +
            "            {'screen_name': 'GOP', 'name': 'GOP', 'id': 11134252, 'id_str': '11134252', 'indices': [294, 298]}, " +
            "            {'screen_name': 'realDonaldTrump', 'name': 'Donald J. Trump', 'id': 25073877, 'id_str': '25073877', 'indices': [305, 321]}" +
            "        ], " +
            "        'symbols': [], " +
            "        'media': [{" +
            "            'id': 1292717294993408001, " +
            "            'id_str': '1292717294993408001', " +
            "            'indices': [322, 345], " +
            "            'media_url': 'http://pbs.twimg.com/tweet_video_thumb/EfCnh9sWAAEkMJ3.jpg', " +
            "            'media_url_https': 'https://pbs.twimg.com/tweet_video_thumb/EfCnh9sWAAEkMJ3.jpg', " +
            "            'url': 'https://t.co/odX0Q5FYjo', " +
            "            'display_url': 'pic.twitter.com/odX0Q5FYjo', " +
            "            'expanded_url': 'https://twitter.com/Nilsalollitasmo/status/1292717307618381825/photo/1', " +
            "            'type': 'animated_gif', " +
            "            'video_info': {" +
            "                'aspect_ratio': [89, 50], " +
            "                'variants': [{'bitrate': 0, 'content_type': 'video/mp4', 'url': 'https://video.twimg.com/tweet_video/EfCnh9sWAAEkMJ3.mp4'}]" +
            "            }, " +
            "            'sizes': {" +
            "                'thumb': {'w': 150, 'h': 150, 'resize': 'crop'}, " +
            "                'large': {'w': 356, 'h': 200, 'resize': 'fit'}, " +
            "                'small': {'w': 356, 'h': 200, 'resize': 'fit'}, " +
            "                'medium': {'w': 356, 'h': 200, 'resize': 'fit'}" +
            "            }" +
            "        }]" +
            "    }, " +
            "    'extended_entities': {" +
            "        'media': [{" +
            "            'id': 1292717294993408001, " +
            "            'id_str': '1292717294993408001', " +
            "            'indices': [322, 345], " +
            "            'media_url': 'http://pbs.twimg.com/tweet_video_thumb/EfCnh9sWAAEkMJ3.jpg', " +
            "            'media_url_https': 'https://pbs.twimg.com/tweet_video_thumb/EfCnh9sWAAEkMJ3.jpg', " +
            "            'url': 'https://t.co/odX0Q5FYjo', " +
            "            'display_url': 'pic.twitter.com/odX0Q5FYjo', " +
            "            'expanded_url': 'https://twitter.com/Nilsalollitasmo/status/1292717307618381825/photo/1', " +
            "            'type': 'animated_gif', " +
            "            'video_info': {" +
            "                'aspect_ratio': [89, 50], " +
            "                'variants': [{'bitrate': 0, 'content_type': 'video/mp4', 'url': 'https://video.twimg.com/tweet_video/EfCnh9sWAAEkMJ3.mp4'}]" +
            "            }, " +
            "            'sizes': {" +
            "                'thumb': {'w': 150, 'h': 150, 'resize': 'crop'}, " +
            "                'large': {'w': 356, 'h': 200, 'resize': 'fit'}, " +
            "                'small': {'w': 356, 'h': 200, 'resize': 'fit'}, " +
            "                'medium': {'w': 356, 'h': 200, 'resize': 'fit'}" +
            "            }" +
            "        }]" +
            "    }" +
            "}, " +
            "'quote_count': 0, " +
            "'reply_count': 0, " +
            "'retweet_count': 0, " +
            "'favorite_count': 0, " +
            "'entities': {" +
            "    'hashtags': [], " +
            "    'urls': [{" +
            "        'url': 'https://t.co/e81q6OKLx9', " +
            "        'expanded_url': 'https://twitter.com/i/web/status/1292717307618381825', " +
            "        'display_url': 'twitter.com/i/web/status/1…', " +
            "        'indices': [117, 140]" +
            "    }], " +
            "    'user_mentions': [" +
            "        {'screen_name': 'doreendibona', 'name': 'Doreen Di Bona', 'id': 738145794, 'id_str': '738145794', 'indices': [0, 13]}, " +
            "        {'screen_name': 'DrGJackBrown', 'name': 'Dr. Jack Brown', 'id': 212445456, 'id_str': '212445456', 'indices': [14, 27]}, " +
            "        {'screen_name': 'marcorubio', 'name': 'Marco Rubio', 'id': 15745368, 'id_str': '15745368', 'indices': [28, 39]}, " +
            "        {'screen_name': 'marcorubio', 'name': 'Marco Rubio', 'id': 15745368, 'id_str': '15745368', 'indices': [40, 51]}" +
            "    ], " +
            "    'symbols': []" +
            "}, " +
            "'favorited': False, " +
            "'retweeted': False, " +
            "'possibly_sensitive': False, " +
            "'filter_level': 'low', " +
            "'lang': 'en', " +
            "'timestamp_ms': '1597042798319'" +
            "}\n";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        USGeoGnosis usGeoGnosis = USGeoGnosisLoader.loadUSGeoGnosis("web/public/data/state.json",
                "web/public/data/county.json",
                "web/public/data/city.json");
        long end = System.currentTimeMillis();
        System.out.println("Initializing USGeoGnosis takes time: " + (end - start) / 1000.0 + " seconds.");

        TwitterGeoTagger.DEBUG = true;

        TwitterGeoTagger.printTagOneTweet(usGeoGnosis, sampleTweet);

        // TODO - Currently, the covid-19 dataset 3 (the complete covid-19 related tweets stream) is being written
        //        to disk in an invalid JSON format (e.g., single-quoted, boolean value is True not true, etc.).
        //        Our TwitterGeoTagger using Jackson json parser can not parse such invalid tweets.
        // TwitterGeoTagger.printTagOneTweet(usGeoGnosis, sampleInvalidTweet);
    }
}
