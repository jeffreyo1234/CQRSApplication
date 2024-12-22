package com.example.demo.listener;

import com.example.demo.command.exceptions.EventProcessingException;
import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventListener {

  private final QueryRepository queryRepository;
  private static final Logger logger = LoggerFactory.getLogger(KafkaEventListener.class);

  @Transactional
  @KafkaListener(
      topics = "user-events",
      groupId = "${spring.kafka.consumer.group-id}",
      errorHandler = "kafkaErrorHandler")
  public void handleUserEvents(@Payload ConsumerRecord<String, Object> record) {
    Object event = record.value();
    String eventId = record.key();
    logger.info("Received event: {} with key: {}", event, eventId);

    try {
      if (record.value() instanceof UserCreatedEvent createdEvent) {
        handleUserCreated(createdEvent);
      } else if (record.value() instanceof UserUpdatedEvent updatedEvent) {
        handleUserUpdated(updatedEvent);
      } else {
        logger.warn("Unknown event type: {}", event.getClass().getName());
      }
    } catch (Exception e) {
      logger.error("Error processing event: {} with key: {}", event, eventId, e);
      throw e; // Let the error handler handle retries
    }
  }

  @Transactional
  private void handleUserCreated(UserCreatedEvent event) {
    logger.info("Handling UserCreatedEvent: {}", event);

    // Try to find existing projection
    Optional<UserProjection> existingProjection = queryRepository.findById(event.getId());

    if (existingProjection.isPresent()) {
      UserProjection projection = existingProjection.get();
      // Handle duplicate - check if it's truly a duplicate or needs update
      if (event.getVersion() > projection.getVersion()) {
        projection.setName(event.getName());
        projection.setEmail(event.getEmail());
        projection.setVersion(event.getVersion());
        queryRepository.save(projection);
        logger.info("Updated existing UserProjection: {}", projection);
      } else {
        logger.info("Skipping duplicate event with same or lower version: {}", event);
      }
    } else {
      try {
        UserProjection userProjection =
            new UserProjection(
                event.getId(), event.getName(), event.getEmail(), event.getVersion());
        queryRepository.save(userProjection);
        logger.info("Saved new UserProjection: {}", userProjection);
      } catch (Exception e) {
        // Handle potential concurrent insert
        logger.error("Failed to save UserProjection: {}", event, e);
        throw new EventProcessingException("Failed to save UserProjection", e);
      }
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
