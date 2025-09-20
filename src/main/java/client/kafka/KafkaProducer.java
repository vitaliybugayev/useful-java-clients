package client.kafka;

import client.mq.MqProducer;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@Slf4j
public final class KafkaProducer implements MqProducer {

    private final org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> producer;

    public KafkaProducer(String kafkaHost) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }

    public synchronized <T extends Message> void send(ProducerRecord<String, T> record) {
        byte[] serializedValue = record.value().toByteArray();

        var byteRecord = new ProducerRecord<>(record.topic(), record.key(), serializedValue);
        record.headers().forEach(header -> byteRecord.headers().add(header));

        producer.send(byteRecord, (metadata, exception) -> {
            if (exception != null) {
                log.error("Send error:\n{}", exception.getMessage());
            } else {
                log.info("Sent message to topic: {}\n{}", metadata.topic(), record.value());
            }
        });
    }

    public synchronized void close() {
        producer.close();
    }

}
