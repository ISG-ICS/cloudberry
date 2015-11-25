package edu.uci.ics.twitter.asterix.convertor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class TwitterStreamStatusToAsterixADMTest {

    String url = getClass().getResource("/sample.json").getPath();

    @Test
    public void testConverter() {
        try (BufferedReader br = new BufferedReader(new FileReader(url))) {
            for (String line; (line = br.readLine()) != null; ) {
                System.out.println(TwitterStreamStatusToAsterixADM.convert(TwitterObjectFactory.createStatus(line)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

}