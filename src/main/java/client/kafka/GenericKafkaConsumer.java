package client.kafka;

import client.mq.MqConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import utils.kafka.GenericKafkaMessageWrapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Slf4j
public final class GenericKafkaConsumer implements MqConsumer {

    private final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;
    private final Map<String, List<GenericKafkaMessageWrapper<String>>> messages =
            Collections.synchronizedMap(new HashMap<>());
    private boolean isRunning = false;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final String groupId = "autotests-" + RandomStringUtils.randomAlphanumeric(5);
    private final String kafkaHost;

    public GenericKafkaConsumer(String kafkaHost) {
        this.kafkaHost = kafkaHost;
        var props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaHost);
        props.put(GROUP_ID_CONFIG, groupId);
        props.put(AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }

    public synchronized void subscribe(List<String> topics) {
        consumer.subscribe(topics);
        log.info("Subscribing to topics: {}", topics);
        waitForAssignment();

        if (!isRunning) {
            startConsumer();
        }
    }

    private void waitForAssignment() {
        Awaitility.await("Partitions assigned")
                .atMost(30, TimeUnit.SECONDS)
                .dontCatchUncaughtExceptions()
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
                var headers = new HashMap<String, String>();
                for (var header : r.headers()) {
                    headers.put(header.key(), new String(header.value()));
                }
                var message = new GenericKafkaMessageWrapper<String>(r.value(), headers, r.key());
                log.info("New message in topic {}:\n{}\nHeaders: {}", r.topic(), r.value(), headers);

                if (!messages.containsKey(r.topic())) {
                    messages.put(r.topic(), Collections.synchronizedList(new ArrayList<>()));
                }
                messages.get(r.topic()).add(message);
            } catch (Exception e) {
                log.error("{} poll error:\n{}", r.topic(), e.getMessage());
            }
        }
        consumer.commitSync();
    }

    @Override
    public synchronized List<String> getMessages(String topic) {
        var result = new ArrayList<String>();
        var objects = messages.getOrDefault(topic, new ArrayList<>());
        for (var o : objects) {
            result.add(o.message());
        }
        return result;
    }

    public synchronized List<GenericKafkaMessageWrapper<String>> getWrappedMessages(String topic) {
        return messages.getOrDefault(topic, new ArrayList<>());
    }

    public synchronized void shutdown() {
        executor.shutdown();
        consumer.close();
        messages.clear();
        isRunning = false;
        KafkaAdminClient.deleteGroup(this.groupId, this.kafkaHost);
        KafkaAdminClient.closeAdminClient(this.kafkaHost);
    }
}
