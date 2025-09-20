package client.kafka;

import client.mq.MqConsumer;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import utils.kafka.KafkaMessageWrapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Slf4j
public final class KafkaConsumerWithSchema implements MqConsumer {

    private final KafkaConsumer<String, byte[]> consumer;
    private final Map<String, Parser<? extends Message>> parserMapping = new HashMap<>();
    private final Map<String, List<KafkaMessageWrapper<? extends Message>>> messages =
            Collections.synchronizedMap(new HashMap<>());
    private boolean isRunning = false;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final String groupId = "autotests-" + RandomStringUtils.randomAlphanumeric(5);
    private KafkaProtobufDeserializer<Message> kafkaProtobufDeserializer;
    private final String kafkaHost;

    public KafkaConsumerWithSchema(String kafkaHost, String schemaUrl) {
        this.kafkaHost = kafkaHost;
        var props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaHost);
        props.put(GROUP_ID_CONFIG, groupId);
        props.put(AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        // Configure KafkaProtobufDeserializer only if schema registry is available
        if (schemaUrl != null && !schemaUrl.isEmpty()) {
            kafkaProtobufDeserializer = new KafkaProtobufDeserializer<>();
            var kafkaProtobufDeserializerProps = new HashMap<String, String>();
            kafkaProtobufDeserializerProps.put(KafkaProtobufDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
            kafkaProtobufDeserializer.configure(kafkaProtobufDeserializerProps, false);
        }

        consumer = new KafkaConsumer<>(props);
    }

    public synchronized <T extends Message> void subscribe(String topic, Parser<T> parser) {
        if (!parserMapping.containsKey(topic)) {
            parserMapping.put(topic, parser);

            var topicsToSubscribe = new ArrayList<>(parserMapping.keySet());
            consumer.subscribe(topicsToSubscribe);
            log.info("Subscribing to topics: {}", topicsToSubscribe);
            waitForAssignment();
            if (!isRunning) {
                startConsumer();
            }
        }
    }

    public synchronized void subscribe(Map<String, Parser<? extends Message>> topics) {
        var topicsToSubscribe = new ArrayList<>(topics.keySet());
        consumer.subscribe(topicsToSubscribe);
        log.info("Subscribing to topics: {}", topicsToSubscribe);
        waitForAssignment();
        parserMapping.putAll(topics);
        if (!isRunning) {
            startConsumer();
        }
    }

    private void waitForAssignment() {
        Awaitility.await("Partitions assigned")
                .atMost(11, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    consumer.poll(Duration.ofMillis(100));
                    var assignment = consumer.assignment();
                    log.debug("Assigned partitions: {}", assignment);
                    Assertions.assertThat(assignment)
                            .as("Check if consumer is assigned to partitions")
                            .isNotEmpty();
                });
    }

    private void startConsumer() {
        executor.scheduleAtFixedRate(this::receive, 0, 200, TimeUnit.MILLISECONDS);
        isRunning = true;
    }

    private synchronized void receive() {
        var records = consumer.poll(Duration.ofMillis(100));

        for (var r : records) {
            try {
                var parser = parserMapping.get(r.topic());
                if (parser != null) {
                    var actualMessage = deserializeWithSchema(r.topic(), r.value());

                    if (actualMessage == null) {
                        actualMessage = r.value();
                    }
                    var parsed = parser.parseFrom(actualMessage);
                    var headers = new HashMap<String, String>();
                    for (var header : r.headers()) {
                        headers.put(header.key(), new String(header.value()));
                    }
                    var wrapper = new KafkaMessageWrapper<>(parsed, headers, r.key());
                    log.info("New message in topic {}:\n{}", r.topic(), wrapper);

                    if (!messages.containsKey(r.topic())) {
                        messages.put(r.topic(), Collections.synchronizedList(new ArrayList<>()));
                    }
                    messages.get(r.topic()).add(wrapper);
                }
            } catch (Exception e) {
                log.error("{} poll error:\n{}", r.topic(), e.getMessage());
            }
        }
        consumer.commitSync();
    }

    private byte[] deserializeWithSchema(String topic, byte[] value) {
        try {
            return kafkaProtobufDeserializer.deserialize(topic, value).toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized <T> List<T> getMessages(String topic) {
        var result = new ArrayList<T>();
        var objects = messages.getOrDefault(topic, new ArrayList<>());
        for (var o : objects) {
            result.add((T) o);
        }
        return result;
    }

    public synchronized void shutdown() {
        executor.shutdown();
        consumer.close();
        parserMapping.clear();
        messages.clear();
        isRunning = false;
        KafkaAdminClient.deleteGroup(this.groupId, this.kafkaHost);
        KafkaAdminClient.closeAdminClient(this.kafkaHost);
    }
}
