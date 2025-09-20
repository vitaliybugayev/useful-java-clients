package utils.kafka;

import lombok.ToString;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

@ToString
public class StringHeader implements Header {
    private final String key;
    private final String value;

    public StringHeader(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String key() {
        return this.key;
    }

    @Override
    public byte[] value() {
        return this.value.getBytes(StandardCharsets.UTF_8);
    }

}
