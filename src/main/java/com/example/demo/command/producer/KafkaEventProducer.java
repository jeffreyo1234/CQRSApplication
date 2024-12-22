package com.example.demo.command.producer;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// This is a producer class that will send messages to the Kafka topic
@RequiredArgsConstructor
@Component
public class KafkaEventProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendUserCreatedEvent(UserCreatedEvent event) {
    kafkaTemplate.send("user-events", event);
  }

  public void sendUserUpdatedEvent(UserUpdatedEvent event) {
    kafkaTemplate.send("user-events", event);
  }
}
