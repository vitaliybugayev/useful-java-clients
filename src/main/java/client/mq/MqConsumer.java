package client.mq;

import java.util.List;

public interface MqConsumer {

    <T> List<T> getMessages(String queueIdentifier);
}
