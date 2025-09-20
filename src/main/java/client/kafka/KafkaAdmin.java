package client.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;

public class KafkaAdmin {

    private final AdminClient client;

    public KafkaAdmin(String host) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, host);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000");
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        client = AdminClient.create(props);
    }

    public void createTopic(String topicName) throws ExecutionException, InterruptedException {
        var partitions = 3;
        var replicationFactor = (short) 3;
        var topic = new NewTopic(topicName, partitions, replicationFactor);
        var createTopicsResult = client.createTopics(List.of(topic));
        createTopicsResult.topicId(topicName).get();
    }

    public void deleteTopic(String topicName) throws ExecutionException, InterruptedException {
        var deleteTopicsResult = client.deleteTopics(List.of(topicName));
        deleteTopicsResult.all().get();
    }

    public void close() {
        client.close();
    }
}
