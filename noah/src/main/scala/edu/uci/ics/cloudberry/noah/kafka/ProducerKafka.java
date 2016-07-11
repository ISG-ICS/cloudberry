package edu.uci.ics.cloudberry.noah.kafka;

import edu.uci.ics.cloudberry.noah.feed.Config;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerKafka {

    public static void store(String topic, String msg, Config config) {
        String server = config.getKafkaServer();
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<String, String>(props);
        ProducerRecord<String, String> data = new ProducerRecord<String, String>(topic, msg);
        producer.send(data);
        producer.close();
    }
}
