package client.mq;

import com.rabbitmq.client.BuiltinExchangeType;

public class UsageExample {

    public void example() throws Exception {
        RabbitConsumer.setAddress("amqp://user:pass@host:5672/vhost");
        var consumer = RabbitConsumer.getInstance();

        // Example of consuming already-bound queue by routing key
        var events = consumer.getMessages("routing.key");
        System.out.println("Received events: " + events.size());
    }
}

