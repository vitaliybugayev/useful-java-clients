package client.kafka;

import java.util.List;

public class UsageExample {

    public void consumerExample() {
        var kafkaHost = "localhost:9092";
        var consumer = new GenericKafkaConsumer(kafkaHost);
        consumer.subscribe(List.of("example-topic"));

        var messages = consumer.getMessages("example-topic");
        System.out.println("Received messages: " + messages.size());

        consumer.shutdown();
    }

    public void adminExample() {
        var kafkaHost = "localhost:9092";
        var admin = new KafkaAdmin(kafkaHost);
        try {
            admin.createTopic("example-topic");
            System.out.println("Topic created");
            admin.deleteTopic("example-topic");
            System.out.println("Topic deleted");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            admin.close();
        }
    }
}

