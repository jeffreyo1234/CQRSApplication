package com.example.demo.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.config.EmbeddedMongoConfig;
import com.example.demo.query.model.UserMongoProjection;
import com.example.demo.query.repo.QueryMongoRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
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
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(EmbeddedMongoConfig.class)
public class KafkaMongoIntegrationTest {

  private static final String TOPIC = "user-events";
  private static final Duration TIMEOUT = Duration.ofSeconds(20);

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired private QueryMongoRepository queryMongoRepository;

  @Autowired private ConsumerFactory<String, Object> consumerFactory;

  private Consumer<String, Object> consumer;

  @BeforeEach
  void setUp() {
    queryMongoRepository.deleteAll();

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
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      try {
        consumer.unsubscribe();
        consumer.close(Duration.ofSeconds(5));
      } catch (Exception e) {
        log.error("Error closing Kafka consumer", e);
      }
    }

    queryMongoRepository.deleteAll();
  }

  @Test
  void testUserCreatedEventSuccess() throws Exception {
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    kafkaTemplate.send(TOPIC, event.getId().toString(), event).get(TIMEOUT.toMillis(), SECONDS);

    await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              UserMongoProjection projection =
                  queryMongoRepository.findById(event.getId()).orElse(null);
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
}
