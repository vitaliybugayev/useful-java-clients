package client.mq;

import com.google.protobuf.Message;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface MqProducer {

    <T extends Message> void send(ProducerRecord<String, T> record);

    void close();
}
