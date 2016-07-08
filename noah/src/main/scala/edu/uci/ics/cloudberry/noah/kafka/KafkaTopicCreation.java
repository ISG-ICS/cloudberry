package edu.uci.ics.cloudberry.noah.kafka;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;

import java.util.Properties;
public class KafkaTopicCreation {

        public static void main(String[] args) throws Exception {
            ZkClient zkClient = null;
            ZkUtils zkUtils = null;
            try {
                String zookeeperHosts = "localhost:2181"; // If multiple zookeeper then -> String zookeeperHosts = "192.168.20.1:2181,192.168.20.2:2181";
                int sessionTimeOutInMs = 15 * 1000; // 15 secs
                int connectionTimeOutInMs = 10 * 1000; // 10 secs

                zkClient = new ZkClient(zookeeperHosts, sessionTimeOutInMs, connectionTimeOutInMs, ZKStringSerializer$.MODULE$);
                zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperHosts), false);

                String topicName = "testTopic";
                int noOfPartitions = 2;
                int noOfReplication = 1;
                Properties topicConfiguration = new Properties();

                AdminUtils.createTopic(zkUtils, topicName, noOfPartitions, noOfReplication, topicConfiguration, RackAwareMode.Enforced$.MODULE$);

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (zkClient != null) {
                    zkClient.close();
                }
            }
        }
    }

