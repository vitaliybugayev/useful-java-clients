package client.mq;

import com.google.protobuf.Message;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.testng.TestException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Slf4j
public final class RabbitConsumer implements MqConsumer {

    private static RabbitConsumer instance;
    private static String uri;
    private final Channel chan;
    private final Map<String, Class<? extends Message>> typeMapping = new HashMap<>();
    private final Map<String, List<Object>> messages = Collections.synchronizedMap(new HashMap<>());

    private RabbitConsumer() {
        var factory = new ConnectionFactory();
        try {
            if (uri == null) {
                throw new TestException("RabbitMQ address is not set");
            }
            factory.setUri(uri);
        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new TestException(e);
        }
        Connection connection;
        try {
            connection = factory.newConnection();
            chan = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new TestException(e);
        }
    }

    public static synchronized RabbitConsumer getInstance() {
        if (instance == null) {
            instance = new RabbitConsumer();
        }
        return instance;
    }

    public static synchronized void setAddress(String address) {
        uri = address;
    }

    public synchronized <T extends Message> void receive(String exchange, BuiltinExchangeType type, String queue,
                                            String routingKey, Class<T> messageType) throws IOException {
        if (typeMapping.containsKey(queue)) {
            return;
        }

        chan.exchangeDeclare(exchange, type);
        chan.queueDeclare(queue, false, false, true, null);
        chan.queueBind(queue, exchange, routingKey);
        typeMapping.put(routingKey, messageType);
        chan.basicConsume(queue, true, new DefaultConsumer(chan) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {
                var type = typeMapping.get(envelope.getRoutingKey());
                var deserialized = deserialize(type, body);
                if (!messages.containsKey(envelope.getRoutingKey())) {
                    messages.put(envelope.getRoutingKey(), Collections.synchronizedList(new ArrayList<>()));
                }
                messages.get(envelope.getRoutingKey()).add(deserialized);

                log.info("New event by {} routing:\n{}", envelope.getRoutingKey(), deserialized);
            }
        });
    }

    private <T> T deserialize(Class<T> type, byte[] data) {
        try {
            var parseFrom = type.getMethod("parseFrom", byte[].class);
            return type.cast(parseFrom.invoke(null, data));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new TestException("Can't parse message");
        }
    }

    public synchronized <T> List<T> getMessages(String routingKey) {
        var result = new ArrayList<T>();
        var objects = messages.getOrDefault(routingKey, new ArrayList<>());
        for (var o : objects) {
            result.add((T) o);
        }
        return result;
    }
}
