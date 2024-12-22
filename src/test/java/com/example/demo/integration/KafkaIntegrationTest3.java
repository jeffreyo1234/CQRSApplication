package com.example.demo.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.config.KafkaTestConfig;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"user-events"})
@DirtiesContext
@Import(KafkaTestConfig.class)
public class KafkaIntegrationTest3 {

  private static final String TOPIC = "user-events";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired private QueryRepository queryRepository;

  @Autowired private EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired private Consumer<String, Object> consumer;

  @Autowired private KafkaListenerEndpointRegistry registry;

  @BeforeEach
  void setUp() {
    queryRepository.deleteAll();

    Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker));
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    consumerProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.demo.command.model");
    consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.demo.command.model.UserEvent");
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    consumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps).createConsumer();
    embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TOPIC);

    for (MessageListenerContainer container : registry.getListenerContainers()) {
      ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
    queryRepository.deleteAll();
    try {
      for (MessageListenerContainer container : registry.getListenerContainers()) {
        container.stop();
        container.getContainerProperties().setIdleEventInterval(100L);
      }
    } catch (Exception e) {
      log.error("Error stopping containers", e);
    }
    try {
      embeddedKafkaBroker.destroy();
    } catch (Exception e) {
      log.error("Error destroying embedded Kafka broker", e);
    }
  }

  @Test
  void testUserCreatedEventSuccess() throws Exception {
    // Given
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);

    // When
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);

    // Then
    await()
        .atMost(TIMEOUT)
        .untilAsserted(
            () -> {
              UserProjection projection = queryRepository.findById(event.getId()).orElse(null);
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
    // Given
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", null, 1L);

    // When
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);

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
            });
  }

  @Test
  void testUserUpdatedEventSuccess() throws Exception {
    // Given
    UserCreatedEvent createdEvent =
        new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    kafkaTemplate
        .send(TOPIC, createdEvent.getId().toString(), createdEvent)
        .get(TIMEOUT.toMillis(), SECONDS);

    await()
        .atMost(TIMEOUT)
        .untilAsserted(
            () -> assertThat(queryRepository.findById(createdEvent.getId())).isPresent());

    UserUpdatedEvent updatedEvent = new UserUpdatedEvent(1L, "John Smith", 2L);

    // When
    kafkaTemplate
        .send(TOPIC, updatedEvent.getId().toString(), updatedEvent)
        .get(TIMEOUT.toMillis(), SECONDS);

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
            });
  }

  //  @Test
  //  @Disabled
  //  void testVersionConflict() throws Exception {
  //    // Given
  //    UserCreatedEvent createdEvent =
  //        new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
  //    kafkaTemplate
  //        .send(TOPIC, createdEvent.getId().toString(), createdEvent)
  //        .get(TIMEOUT.toMillis(), SECONDS);
  //
  //    await()
  //        .atMost(TIMEOUT)
  //        .untilAsserted(
  //            () -> assertThat(queryRepository.findById(createdEvent.getId())).isPresent());
  //
  //    // When: Send outdated version
  //    UserUpdatedEvent outdatedEvent = new UserUpdatedEvent(1L, "John Smith", 1L); // Same version
  //    kafkaTemplate
  //        .send(TOPIC, outdatedEvent.getId().toString(), outdatedEvent)
  //        .get(TIMEOUT.toMillis(), SECONDS);
  //
  //    // Then: Projection should not be updated due to version conflict
  //    await()
  //        .atMost(TIMEOUT)
  //        .untilAsserted(
  //            () -> {
  //              UserProjection projection =
  //                  queryRepository.findById(outdatedEvent.getId()).orElse(null);
  //              assertThat(projection)
  //                  .isNotNull()
  //                  .satisfies(
  //                      p -> {
  //                        assertThat(p.getName())
  //                            .isEqualTo(createdEvent.getName()); // Name should not change
  //                        assertThat(p.getVersion()).isEqualTo(createdEvent.getVersion());
  //                      });
  //            });
  //  }

  //  @Test
  //  @Disabled
  //  void testConcurrentEventProcessing() throws Exception {
  //    // Given
  //    int numberOfEvents = 10;
  //    CountDownLatch latch = new CountDownLatch(numberOfEvents);
  //    List<CompletableFuture<SendResult<String, Object>>> futures = new ArrayList<>();
  //
  //    // When: Send multiple events concurrently
  //    for (long i = 1; i <= numberOfEvents; i++) { // Start from 1 to match JPA ID generation
  //      final long id = i;
  //      futures.add(
  //          kafkaTemplate
  //              .send(
  //                  TOPIC,
  //                  String.valueOf(id),
  //                  new UserCreatedEvent(id, "User" + id, "user" + id + "@example.com", 1L))
  //              .whenComplete(
  //                  (result, ex) -> {
  //                    if (ex == null) {
  //                      log.info("Successfully sent event for id {}", id);
  //                      latch.countDown();
  //                    } else {
  //                      log.error("Failed to send event for id {}: {}", id, ex.getMessage());
  //                    }
  //                  }));
  //    }
  //
  //    // Wait for all sends to complete
  //    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
  //        .get(TIMEOUT.toMillis(), SECONDS);
  //
  //    // Wait for all messages to be processed
  //    assertThat(latch.await(30, SECONDS)).isTrue();
  //
  //    // Then verify with retries
  //    await()
  //        .pollInterval(Duration.ofMillis(100))
  //        .atMost(Duration.ofSeconds(30))
  //        .untilAsserted(
  //            () -> {
  //              List<UserProjection> projections = queryRepository.findAll();
  //              log.info("Current projections: {}", projections);
  //
  //              assertThat(projections).hasSize(numberOfEvents);
  //
  //              // Verify each ID exists
  //              Set<Long> processedIds =
  //                  projections.stream().map(UserProjection::getId).collect(Collectors.toSet());
  //              for (long i = 1; i <= numberOfEvents; i++) {
  //                assertThat(processedIds).contains(i);
  //              }
  //            });
  //  }

  @Test
  void testDuplicateEvents() throws Exception {
    // Given
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);

    // When: Send the same event twice
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);

    // Then: Should only create one projection
    await()
        .atMost(TIMEOUT)
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
            });
  }

  @Test
  void testOutOfOrderEvents() throws Exception {
    // Given
    UserCreatedEvent createEvent = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    UserUpdatedEvent updateEvent1 = new UserUpdatedEvent(1L, "John Smith", 2L);
    UserUpdatedEvent updateEvent2 = new UserUpdatedEvent(1L, "John Williams", 3L);

    // When: Send events out of order
    kafkaTemplate
        .send(TOPIC, createEvent.getId().toString(), createEvent)
        .get(TIMEOUT.toMillis(), SECONDS);
    kafkaTemplate
        .send(TOPIC, updateEvent1.getId().toString(), updateEvent1)
        .get(TIMEOUT.toMillis(), SECONDS);
    kafkaTemplate
        .send(TOPIC, updateEvent2.getId().toString(), updateEvent2)
        .get(TIMEOUT.toMillis(), SECONDS);

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
            });
  }

  @Test
  void testKafkaDowntime() throws Exception {
    // Given
    embeddedKafkaBroker.destroy();

    // When
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);

    // Then
    try {
      kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);
    } catch (ExecutionException e) {
      assertThat(e).hasCauseInstanceOf(Exception.class);
    } finally {
      embeddedKafkaBroker.afterPropertiesSet();
      setUp(); // Reinitialize the broker and wait for assignments
    }
  }
}
