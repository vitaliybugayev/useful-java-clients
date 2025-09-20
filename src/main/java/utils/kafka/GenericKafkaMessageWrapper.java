package utils.kafka;

import java.util.Map;

public record GenericKafkaMessageWrapper<T>(T message, Map<String, String> headers, String key) {

    @Override
    public String toString() {
        return """
                message=%s
                headers=%s
                key=%s
                """.formatted(message, headers, key);
    }
}
