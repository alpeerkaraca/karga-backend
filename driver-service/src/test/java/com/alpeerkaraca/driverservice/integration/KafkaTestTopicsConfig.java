// file: driver-service/src/test/java/com/alpeerkaraca/driverservice/integration/KafkaTestTopicsConfig.java
package com.alpeerkaraca.driverservice.integration;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class KafkaTestTopicsConfig {

    @Bean
    KafkaAdmin kafkaAdmin(org.springframework.core.env.Environment env) {
        String bootstrap = env.getProperty("spring.kafka.bootstrap-servers");
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        return new KafkaAdmin(configs);
    }

    @Bean
    NewTopic tripEventsTopic() {
        return new NewTopic("trip_events", 1, (short) 1);
    }

    @Bean
    NewTopic driverLocationUpdatesTopic() {
        return new NewTopic("driver_location_updates", 1, (short) 1);
    }
}