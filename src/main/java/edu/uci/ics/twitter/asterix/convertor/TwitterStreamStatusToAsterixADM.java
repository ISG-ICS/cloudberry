package edu.uci.ics.twitter.asterix.convertor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.uci.ics.twitter.asterix.adm.Tweet;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

public class TwitterStreamStatusToAsterixADM {
    public static String convert(Status status) {
        return Tweet.toADM(status);
    }

    public static void main(String args[]) {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        try {
            for (String line; (line = stdin.readLine()) != null; ) {
                try {
                    System.out.println(convert(TwitterObjectFactory.createStatus(line)));
                } catch (Exception e) {
                    System.err.println("error when parse line:" + line);
                    e.printStackTrace(System.err);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
