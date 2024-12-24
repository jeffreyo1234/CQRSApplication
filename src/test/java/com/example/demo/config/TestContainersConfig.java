package com.example.demo.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

  @Bean(destroyMethod = "")
  public KafkaContainer kafkaContainer() {
    KafkaContainer kafkaContainer =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withEnv("KAFKA_NUM_PARTITIONS", "1")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", "1")
            .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
    kafkaContainer.start();
    return kafkaContainer;
  }

  @Bean
  public KafkaAdmin admin(KafkaContainer kafkaContainer) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopic userEventsTopic() {
    return TopicBuilder.name("user-events")
        .partitions(1)
        .replicas(1)
        .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
        .build();
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(KafkaContainer kafkaContainer) {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

    DefaultKafkaProducerFactory<String, Object> producerFactory =
        new DefaultKafkaProducerFactory<>(props);
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  @Scope("prototype")
  public Consumer<String, Object> testConsumer(KafkaContainer kafkaContainer) {
    Map<String, Object> consumerProps = new HashMap<>();
    consumerProps.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    consumerProps.put(
        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.demo.command.model");
    consumerProps.put(
        JsonDeserializer.TYPE_MAPPINGS,
        "UserCreatedEvent:com.example.demo.command.model.UserCreatedEvent,"
            + "UserUpdatedEvent:com.example.demo.command.model.UserUpdatedEvent");
    consumerProps.put(
        JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.demo.command.model.UserEvent");
    consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

    DefaultKafkaConsumerFactory<String, Object> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    return consumerFactory.createConsumer();
  }
}
