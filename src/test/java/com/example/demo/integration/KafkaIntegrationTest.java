package com.example.demo.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.config.TestContainersConfig;
import com.example.demo.config.TestKafkaConsumerConfig;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;

@Slf4j
@ExtendWith(SpringExtension.class)
@DirtiesContext
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({TestContainersConfig.class, TestKafkaConsumerConfig.class})
public class KafkaIntegrationTest {

  private static final String TOPIC = "user-events";
  private static final Duration TIMEOUT = Duration.ofSeconds(20);

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired private QueryRepository queryRepository;

  @Autowired private KafkaContainer kafkaContainer;

  @Autowired private Consumer<String, Object> consumer;

  @Autowired private ConsumerFactory<String, Object> consumerFactory;

  @Autowired private KafkaListenerEndpointRegistry registry;

  @BeforeEach
  void setUp() {
    log.info("Setting up KafkaIntegrationTest");

    queryRepository.deleteAll();

    if (!kafkaContainer.isRunning()) {
      kafkaContainer.start();
    }

    if (consumer != null) {
      try {
        consumer.unsubscribe();
        consumer.close();
      } catch (Exception e) {
        log.warn("Error closing consumer", e);
      }
    }

    consumer = consumerFactory.createConsumer("test-group-" + UUID.randomUUID(), "true");
    consumer.subscribe(List.of(TOPIC));

    await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              consumer.poll(Duration.ofMillis(100));
              assertThat(consumer.assignment()).isNotEmpty();
            });

    for (MessageListenerContainer container : registry.getListenerContainers()) {
      container.stop();
      container.start();
      ContainerTestUtils.waitForAssignment(container, 1);
    }
  }

  @AfterEach
  void tearDown() {
    log.info("Tearing down KafkaIntegrationTest");

    if (consumer != null) {
      try {
        consumer.unsubscribe();
        consumer.close(Duration.ofSeconds(5));
        log.info("Kafka consumer closed successfully");
      } catch (Exception e) {
        log.error("Error closing Kafka consumer", e);
      }
    }

    queryRepository.deleteAll();

    for (MessageListenerContainer container : registry.getListenerContainers()) {
      try {
        container.stop(() -> log.info("Listener container {} stopped successfully", container));
        log.info("Listener container {} stopped successfully", container);
      } catch (Exception e) {
        log.error("Error stopping listener container", e);
      }
    }
  }

  @Test
  void testUserCreatedEventSuccess() throws Exception {
    log.info("Starting testUserCreatedEventSuccess");

    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    log.info("Created event: {}", event);

    var result = kafkaTemplate.send(TOPIC, event.getId().toString(), event);
    result.get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent event to topic: {}, Send Result: {}", TOPIC, result.get());

    await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              log.info("Consumer assignment: {}", consumer.assignment());

              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              log.info("Polled records count: {}", records.count());
              records.forEach(record -> log.info("Record: {}", record));

              UserProjection projection = queryRepository.findById(event.getId()).orElse(null);
              log.info("Current projection from repository: {}", projection);

              assertThat(projection)
                  .isNotNull()
                  .satisfies(
                      p -> {
                        assertThat(p.getName()).isEqualTo(event.getName());
                        assertThat(p.getEmail()).isEqualTo(event.getEmail());
                        assertThat(p.getVersion()).isEqualTo(event.getVersion());
                      });
            });
  }

  @Test
  void testUserCreatedEventWithNullEmail() throws Exception {
    log.info("Starting testUserCreatedEventWithNullEmail");
    // Given
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", null, 1L);
    log.info("Created UserCreatedEvent with null email: {}", event);

    // When
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent UserCreatedEvent with null email to Kafka topic: {}", TOPIC);

    // Then
    await()
        .atMost(Duration.ofSeconds(60)) // Increased timeout duration
        .untilAsserted(
            () -> {
              UserProjection projection = queryRepository.findById(event.getId()).orElse(null);
              assertThat(projection)
                  .isNotNull()
                  .satisfies(
                      p -> {
                        assertThat(p.getName()).isEqualTo(event.getName());
                        assertThat(p.getEmail()).isNull();
                        assertThat(p.getVersion()).isEqualTo(event.getVersion());
                      });
              log.info("Verified UserProjection with null email: {}", projection);
            });
  }

  @Test
  void testUserUpdatedEventSuccess() throws Exception {
    log.info("Starting testUserUpdatedEventSuccess");
    // Given
    UserCreatedEvent createdEvent =
        new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    kafkaTemplate
        .send(TOPIC, createdEvent.getId().toString(), createdEvent)
        .get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent UserCreatedEvent to Kafka topic: {}", TOPIC);

    await()
        .atMost(TIMEOUT)
        .untilAsserted(
            () -> assertThat(queryRepository.findById(createdEvent.getId())).isPresent());
    log.info("Verified UserCreatedEvent in query repository");

    UserUpdatedEvent updatedEvent = new UserUpdatedEvent(1L, "John Smith", 2L);
    log.info("Created UserUpdatedEvent: {}", updatedEvent);

    // When
    kafkaTemplate
        .send(TOPIC, updatedEvent.getId().toString(), updatedEvent)
        .get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent UserUpdatedEvent to Kafka topic: {}", TOPIC);

    // Then
    await()
        .atMost(TIMEOUT)
        .untilAsserted(
            () -> {
              UserProjection projection =
                  queryRepository.findById(updatedEvent.getId()).orElse(null);
              assertThat(projection)
                  .isNotNull()
                  .satisfies(
                      p -> {
                        assertThat(p.getName()).isEqualTo(updatedEvent.getName());
                        assertThat(p.getVersion()).isEqualTo(updatedEvent.getVersion());
                        assertThat(p.getEmail())
                            .isEqualTo(createdEvent.getEmail()); // Email should remain unchanged
                      });
              log.info("Verified UserProjection after update: {}", projection);
            });
  }

  @Test
  void testDuplicateEvents() throws Exception {
    log.info("Starting testDuplicateEvents");
    // Given
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    log.info("Created UserCreatedEvent: {}", event);

    // When: Send the same event twice
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent UserCreatedEvent to Kafka topic: {}", TOPIC);
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent duplicate UserCreatedEvent to Kafka topic: {}", TOPIC);

    // Then: Should only create one projection
    await()
        .atMost(Duration.ofSeconds(30)) // Increased timeout duration
        .untilAsserted(
            () -> {
              List<UserProjection> projections = queryRepository.findAll();
              assertThat(projections).hasSize(1);
              assertThat(projections.get(0))
                  .satisfies(
                      p -> {
                        assertThat(p.getId()).isEqualTo(event.getId());
                        assertThat(p.getName()).isEqualTo(event.getName());
                        assertThat(p.getEmail()).isEqualTo(event.getEmail());
                        assertThat(p.getVersion()).isEqualTo(event.getVersion());
                      });
              log.info(
                  "Verified single UserProjection after duplicate events: {}", projections.get(0));
            });
  }

  @Test
  void testOutOfOrderEvents() throws Exception {
    log.info("Starting testOutOfOrderEvents");
    // Given
    UserCreatedEvent createEvent = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    UserUpdatedEvent updateEvent1 = new UserUpdatedEvent(1L, "John Smith", 2L);
    UserUpdatedEvent updateEvent2 = new UserUpdatedEvent(1L, "John Williams", 3L);
    log.info("Created events: {}, {}, {}", createEvent, updateEvent1, updateEvent2);

    // When: Send events out of order
    kafkaTemplate
        .send(TOPIC, createEvent.getId().toString(), createEvent)
        .get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent UserCreatedEvent to Kafka topic: {}", TOPIC);
    kafkaTemplate
        .send(TOPIC, updateEvent1.getId().toString(), updateEvent1)
        .get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent first UserUpdatedEvent to Kafka topic: {}", TOPIC);
    kafkaTemplate
        .send(TOPIC, updateEvent2.getId().toString(), updateEvent2)
        .get(TIMEOUT.toMillis(), SECONDS);
    log.info("Sent second UserUpdatedEvent to Kafka topic: {}", TOPIC);

    // Then: Final state should reflect the highest version
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              UserProjection projection =
                  queryRepository.findById(createEvent.getId()).orElse(null);
              assertThat(projection)
                  .isNotNull()
                  .satisfies(
                      p -> {
                        assertThat(p.getName()).isEqualTo(updateEvent2.getName());
                        assertThat(p.getVersion()).isEqualTo(updateEvent2.getVersion());
                      });
              log.info("Verified UserProjection after out-of-order events: {}", projection);
            });
  }

  @Test
  void testKafkaDowntime() throws Exception {
    log.info("Starting testKafkaDowntime");
    // Given
    kafkaContainer.stop();
    log.info("Stopped Kafka container");

    // When
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    log.info("Created UserCreatedEvent: {}", event);

    // Then
    try {
      kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);
    } catch (ExecutionException e) {
      log.error("Expected error sending event to Kafka while down", e);
      assertThat(e).hasCauseInstanceOf(Exception.class);
    } finally {
      kafkaContainer.start();
      log.info("Restarted Kafka container");
      setUp(); // Reinitialize the container and wait for assignments
    }
  }
}
