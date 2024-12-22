package com.example.demo.service;

import com.example.demo.command.model.User;
import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.command.producer.KafkaEventProducer;
import com.example.demo.command.repository.UserRepository;
import com.example.demo.command.service.UserCommandServiceKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserCommandServiceKafkaProducerTests {

  @Mock private UserRepository userRepository;

  @Mock private KafkaEventProducer kafkaEventProducer;

  @InjectMocks private UserCommandServiceKafkaProducer userCommandServiceKafkaProducer;

  @Captor private ArgumentCaptor<UserCreatedEvent> userCreatedEventCaptor;

  @Captor private ArgumentCaptor<UserUpdatedEvent> userUpdatedEventCaptor;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  class CreateUserTests {
    @Test
    void testCreateUser() {
      User user = new User(null, "John Doe", "john.doe@example.com", null);
      User savedUser = new User(1L, "John Doe", "john.doe@example.com", 1L);

      when(userRepository.save(any(User.class))).thenReturn(savedUser);

      User result = userCommandServiceKafkaProducer.createUser(user);

      assertNotNull(result);
      assertEquals(savedUser.getId(), result.getId());
      assertEquals(savedUser.getName(), result.getName());
      assertEquals(savedUser.getEmail(), result.getEmail());
      assertEquals(savedUser.getVersion(), result.getVersion());

      verify(userRepository, times(1)).save(user);
      verify(kafkaEventProducer, times(1)).sendUserCreatedEvent(userCreatedEventCaptor.capture());

      UserCreatedEvent capturedEvent = userCreatedEventCaptor.getValue();
      assertNotNull(capturedEvent);
      assertEquals(savedUser.getId(), capturedEvent.getId());
      assertEquals(savedUser.getName(), capturedEvent.getName());
      assertEquals(savedUser.getEmail(), capturedEvent.getEmail());
      assertEquals(savedUser.getVersion(), capturedEvent.getVersion());
    }
  }

  @Nested
  class UpdateUserTests {
    @Test
    void testUpdateUser() {
      User existingUser = new User(1L, "John Doe", "john.doe@example.com", 1L);
      User updatedUser = new User(1L, "John Doe Updated", "john.doe.updated@example.com", 2L);

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
      when(userRepository.save(any(User.class))).thenReturn(updatedUser);

      User result = userCommandServiceKafkaProducer.updateUser(existingUser.getId(), updatedUser);

      assertNotNull(result);
      assertEquals(updatedUser.getId(), result.getId());
      assertEquals(updatedUser.getName(), result.getName());
      assertEquals(updatedUser.getEmail(), result.getEmail());
      assertEquals(updatedUser.getVersion(), result.getVersion());

      verify(userRepository, times(1)).findById(existingUser.getId());
      verify(userRepository, times(1)).save(existingUser);
      verify(kafkaEventProducer, times(1)).sendUserUpdatedEvent(userUpdatedEventCaptor.capture());

      UserUpdatedEvent capturedEvent = userUpdatedEventCaptor.getValue();
      assertNotNull(capturedEvent);
      assertEquals(updatedUser.getId(), capturedEvent.getId());
      assertEquals(updatedUser.getName(), capturedEvent.getName());
      assertEquals(updatedUser.getVersion(), capturedEvent.getVersion());
    }
  }

  @Nested
  class DeleteUserTests {
    @Test
    void testDeleteUser() {
      Long userId = 1L;

      doNothing().when(userRepository).deleteById(userId);

      userCommandServiceKafkaProducer.deleteUser(userId);

      verify(userRepository, times(1)).deleteById(userId);
    }
  }
}
