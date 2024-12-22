package com.example.demo.listener;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KafkaEventListenerTests {

  @Mock
  private QueryRepository queryRepository;

  @InjectMocks
  private KafkaEventListener kafkaEventListener;

  @Captor
  private ArgumentCaptor<UserProjection> userProjectionCaptor;

  private static final Logger logger = LoggerFactory.getLogger(KafkaEventListenerTests.class);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testHandleUserCreatedEvent() {
    UserCreatedEvent event = new UserCreatedEvent(1L, "John Doe", "john.doe@example.com", 1L);
    ConsumerRecord<String, Object> record = new ConsumerRecord<>("user-events", 0, 0L, null, event);

    when(queryRepository.existsById(event.getId())).thenReturn(false);

    kafkaEventListener.handleUserEvents(record);

    verify(queryRepository, times(1)).save(userProjectionCaptor.capture());
    UserProjection savedProjection = userProjectionCaptor.getValue();

    assertNotNull(savedProjection);
    assertEquals(event.getId(), savedProjection.getId());
    assertEquals(event.getName(), savedProjection.getName());
    assertEquals(event.getEmail(), savedProjection.getEmail());
    assertEquals(event.getVersion(), savedProjection.getVersion());
  }

  @Test
  public void testHandleUserUpdatedEvent() {
    UserUpdatedEvent event = new UserUpdatedEvent(1L, "John Doe Updated", 2L);
    ConsumerRecord<String, Object> record = new ConsumerRecord<>("user-events", 0, 0L, null, event);

    UserProjection existingProjection = new UserProjection(1L, "John Doe", "john.doe@example.com", 1L);
    when(queryRepository.findById(event.getId())).thenReturn(Optional.of(existingProjection));

    kafkaEventListener.handleUserEvents(record);

    verify(queryRepository, times(1)).save(userProjectionCaptor.capture());
    UserProjection updatedProjection = userProjectionCaptor.getValue();

    assertNotNull(updatedProjection);
    assertEquals(event.getId(), updatedProjection.getId());
    assertEquals(event.getName(), updatedProjection.getName());
    assertEquals(existingProjection.getEmail(), updatedProjection.getEmail());
    assertEquals(event.getVersion(), updatedProjection.getVersion());
  }

  @Test
  public void testHandleUnknownEvent() {
    Object unknownEvent = new Object();
    ConsumerRecord<String, Object> record = new ConsumerRecord<>("user-events", 0, 0L, null, unknownEvent);

    kafkaEventListener.handleUserEvents(record);

    verify(queryRepository, never()).save(any(UserProjection.class));
    logger.warn("Unknown event type: {}", unknownEvent.getClass().getName());
  }
}