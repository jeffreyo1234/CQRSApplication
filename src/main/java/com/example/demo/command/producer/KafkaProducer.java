package com.example.demo.command.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// This is a producer class that will send messages to the Kafka topic
@RequiredArgsConstructor
@Component
public class KafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendUserCreatedEvent(Object event) {
    kafkaTemplate.send("user-events", event);
  }

  public void sendUserUpdatedEvent(Object event) {
    kafkaTemplate.send("user-events", event);
  }
}
