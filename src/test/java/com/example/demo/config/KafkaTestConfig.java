package com.example.demo.config;

import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@TestConfiguration
public class KafkaTestConfig {

  @Bean
  public Map<String, Object> producerConfigs() {
    return KafkaTestUtils.producerProps("localhost:9092");
  }

  @Bean
  public DefaultKafkaProducerFactory<String, Object> producerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfigs());
  }

  @Bean
  public Map<String, Object> consumerConfigs() {
    return KafkaTestUtils.consumerProps("test-group", "true", "localhost:9092");
  }

  @Bean
  public DefaultKafkaConsumerFactory<String, Object> consumerFactory() {
    return new DefaultKafkaConsumerFactory<>(consumerConfigs());
  }
}
