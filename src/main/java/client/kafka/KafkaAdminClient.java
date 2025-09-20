package client.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;

@Slf4j
public final class KafkaAdminClient {
    private static final Map<String, AdminClient> ADMIN_CLIENTS = new HashMap<>();

    private static Properties getAdminProps(String kafkaHost) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaHost);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000");
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        return props;
    }

    private static synchronized AdminClient getAdminClient(String kafkaHost) {
        if (!ADMIN_CLIENTS.containsKey(kafkaHost)) {
            ADMIN_CLIENTS.put(kafkaHost, AdminClient.create(getAdminProps(kafkaHost)));
        }
        return ADMIN_CLIENTS.get(kafkaHost);
    }

    public static synchronized void deleteGroup(String groupToDelete, String kafkaHost) {
        try {
            getAdminClient(kafkaHost).deleteConsumerGroups(List.of(groupToDelete)).all().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Can't delete groups: {}", e.getMessage());
        }
    }

    public static synchronized void deleteGroups(List<String> groupsToDelete, String kafkaHost) {
        try {
            getAdminClient(kafkaHost).deleteConsumerGroups(groupsToDelete).all().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Can't delete group: {}", e.getMessage());
        }
    }

    public static synchronized Collection<ConsumerGroupListing> getAllConsumerGroups(String kafkaHost) {
        try {
            return getAdminClient(kafkaHost).listConsumerGroups().all().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Can't get groups: {}", e.getMessage());
        }
        return null;
    }

    public static synchronized void closeAdminClient(String instanceId) {
        AdminClient client = ADMIN_CLIENTS.get(instanceId);
        if (client != null) {
            client.close();
            ADMIN_CLIENTS.remove(instanceId);
        }
    }

    public static synchronized void closeAllAdminClients() {
        for (AdminClient client : ADMIN_CLIENTS.values()) {
            client.close();
        }
        ADMIN_CLIENTS.clear();
    }
}
