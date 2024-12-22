package com.example.demo.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@TestConfiguration
@EmbeddedKafka(
    partitions = 1,
    topics = {"user-events"})
public class KafkaTestConfig {

  @Bean
  public EmbeddedKafkaBroker embeddedKafkaBroker() {
    EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, 1, "user-events");
    broker.brokerProperty("listeners", "PLAINTEXT://localhost:0"); // Use an ephemeral port
    return broker;
  }

  @Bean
  public Consumer<String, Object> testConsumer(EmbeddedKafkaBroker embeddedKafkaBroker) {
    Map<String, Object> consumerProps =
        new HashMap<>(KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker));
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    consumerProps.put(
        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.demo.command.model");
    consumerProps.put(
        JsonDeserializer.TYPE_MAPPINGS,
        "UserCreatedEvent:com.example.demo.command.model.UserCreatedEvent,UserUpdatedEvent:com.example.demo.command.model.UserUpdatedEvent");
    consumerProps.put(
        JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.demo.command.model.UserEvent");
    consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

    DefaultKafkaConsumerFactory<String, Object> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    return consumerFactory.createConsumer();
  }
}
