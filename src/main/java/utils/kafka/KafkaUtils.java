package utils.kafka;

import com.google.protobuf.Message;
import org.apache.kafka.common.header.internals.RecordHeaders;
import utils.UtilityClass;

import java.util.List;
import java.util.Optional;

public final class KafkaUtils {

    private KafkaUtils() {
        UtilityClass.getException();
    }

    private static final String TRACE_ID_KEY = "x-trace-id";
    private static final String IDEMPOTENCY_ID_KEY = "x-idempotency-id";

    public static RecordHeaders getHeaders(String traceId) {
        var split = traceId.split(":");
        var idempotencyId = split[split.length - 1];
        return getHeaders(traceId, idempotencyId);
    }

    public static RecordHeaders getHeaders(String traceId, String idempotencyId) {
        var headers = new RecordHeaders();
        headers.add(new StringHeader(TRACE_ID_KEY, traceId));
        headers.add(new StringHeader(IDEMPOTENCY_ID_KEY, idempotencyId));
        return headers;
    }

    public static <T extends Message> Optional<T> getMessageByTraceId(List<KafkaMessageWrapper<T>> messages,
                                                                      String traceId) {
        return messages.stream()
                .filter(message -> {
                    var headers = message.headers();
                    var trace = headers.get(TRACE_ID_KEY);
                    return traceId.equals(trace);
                })
                .findFirst()
                .map(KafkaMessageWrapper::message);
    }
}
