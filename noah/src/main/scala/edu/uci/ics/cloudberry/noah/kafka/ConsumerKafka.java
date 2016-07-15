package edu.uci.ics.cloudberry.noah.kafka;


import edu.uci.ics.cloudberry.noah.feed.AsterixHttpRequest;
import edu.uci.ics.cloudberry.noah.feed.Config;
import edu.uci.ics.cloudberry.noah.feed.TagTweetGeotagNotRequired;
import edu.uci.ics.cloudberry.noah.feed.TwitterFeedStreamDriver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.kohsuke.args4j.CmdLineException;
import twitter4j.TwitterException;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerKafka {

    private Properties getProperties(String server, String groupId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    private void ingest(Config config, String msg) {
        TwitterFeedStreamDriver feedDriver = new TwitterFeedStreamDriver();
        try {
            feedDriver.openSocket(config);
            try {
                String adm = TagTweetGeotagNotRequired.tagOneTweet(msg);
                feedDriver.socketAdapterClient.ingest(adm);
            } catch (TwitterException e) {
                e.printStackTrace(System.err);
            }
        } catch (CmdLineException e) {
            e.printStackTrace(System.err);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (feedDriver.socketAdapterClient != null) {
                feedDriver.socketAdapterClient.finalize();
            }
        }
    }

    public void run(Config config, String[] topics, String dataset) throws CmdLineException {
        String server = config.getKafkaServer();
        String groupId = config.getKafkaId();
        Properties props = this.getProperties(server, groupId);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topics));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            if (dataset.equals("ds_zika_streaming")) {
                for (ConsumerRecord<String, String> record : records) {
                    ingest(config, record.value());
                }
            } else {
                for (ConsumerRecord<String, String> record : records) {
                    AsterixHttpRequest.insertDB("twitter_zika", dataset, record.value());
                }
            }
        }
    }
}