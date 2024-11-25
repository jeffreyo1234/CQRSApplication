package com.example.demo.listener;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// This is a listener class that will listen to the Kafka topic
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

  private final QueryRepository queryRepository;

  @KafkaListener(topics = "user-events", groupId = "query-service")
  public void handleUserEvents(Object event) {
    if (event instanceof UserCreatedEvent createdEvent) {
      handleUserCreated(createdEvent);
    } else if (event instanceof UserUpdatedEvent updatedEvent) {
      handleUserUpdated(updatedEvent);
    }
  }

  // This method will be called when a UserCreatedEvent is published
  private void handleUserCreated(UserCreatedEvent event) {
    if (!queryRepository.existsById(event.getId())) {
      queryRepository.save(
          new UserProjection(event.getId(), event.getName(), event.getEmail(), event.getVersion()));
    }
  }

  // This method will be called when a UserUpdatedEvent is published
  private void handleUserUpdated(UserUpdatedEvent event) {
    UserProjection projection =
        queryRepository
            .findById(event.getId())
            .orElseThrow(() -> new RuntimeException("UserProjection not found"));
    if (event.getVersion() > projection.getVersion()) {
      projection.setName(event.getName());
      projection.setVersion(event.getVersion());
      queryRepository.save(projection);
    }
  }
}
