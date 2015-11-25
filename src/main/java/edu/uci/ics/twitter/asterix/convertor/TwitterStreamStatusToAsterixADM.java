package edu.uci.ics.twitter.asterix.convertor;

import edu.uci.ics.twitter.asterix.adm.Tweet;
import twitter4j.Status;

public class TwitterStreamStatusToAsterixADM {
    public static String convert(Status status) {
        return Tweet.toADM(status);
    }

}
