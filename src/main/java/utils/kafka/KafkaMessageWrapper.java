package utils.kafka;

import com.google.protobuf.Message;

import java.util.Map;

public record KafkaMessageWrapper<T extends Message>(T message, Map<String, String> headers, String key) {

    @Override
    public String toString() {
        return """
                message=%s
                headers=%s
                key=%s
                """.formatted(message, headers, key);
    }
}
