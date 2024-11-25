package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"user-events"})
class KafkaIntegrationTest {

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired private QueryRepository queryRepository;

  @Test
  void testEndToEndKafkaWorkflow() throws Exception {
    // Arrange
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john@example.com", 1L);

    // Act
    kafkaTemplate.send("user-events", "UserCreated", event).get();
    Thread.sleep(5000); // Allow consumer to process the event

    // Assert
    UserProjection projection = queryRepository.findById(1L).orElse(null);
    assertThat(projection).isNotNull();
    assertThat(projection.getName()).isEqualTo("John Doe");
  }
}
