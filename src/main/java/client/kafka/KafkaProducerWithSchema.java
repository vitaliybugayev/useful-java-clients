package client.kafka;

import client.mq.MqProducer;
import com.google.protobuf.Message;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@Slf4j
public final class KafkaProducerWithSchema implements MqProducer {

    private final org.apache.kafka.clients.producer.KafkaProducer<String, ? extends Message> producer;

    public KafkaProducerWithSchema(String kafkaHost, String schemaUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class.getName());
        props.put(KafkaProtobufDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }

    public synchronized <T extends Message> void send(ProducerRecord<String, T> record) {
        org.apache.kafka.clients.producer.KafkaProducer<String, T> typedProducer =
                (org.apache.kafka.clients.producer.KafkaProducer<String, T>) producer;
        typedProducer.send(record, (metadata, exception) -> {
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
