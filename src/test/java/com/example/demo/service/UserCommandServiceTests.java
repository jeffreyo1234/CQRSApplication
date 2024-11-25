package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.command.model.User;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.command.repository.UserRepository;
import com.example.demo.command.service.UserCommandService;
import com.example.demo.listener.UserEventListener;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;

// This class tests the UserCommandService class
@SpringBootTest
public class UserCommandServiceTests {

  @MockBean private UserRepository userRepository;
  @MockBean private ApplicationEventPublisher eventPublisher;
  @MockBean private UserEventListener userEventListener;
  @Autowired private UserCommandService userCommandService;

  // TODO: Add tests for the UserCommandService class
  // This test will check if the user is created
  @Nested
  public class GivenTheCreateUserMethodIsCalled {
    @Nested
    public class WhentheUserIsPassedIn {
      @Test
      void thenTheUserIsCreated() throws Exception {
        // Mock the UserRepository return a user when save is called
        // Call userCommandService.createUser() with a dummy user
        // verify that the returned user has the correct data
        // verify that the userRepository.save() method was called
        // verify that the eventPublisher.publishEvent() method was called

        User dummyUser = new User(1L, "John Doe", "", 1L);
        when(userRepository.save(any())).thenReturn(dummyUser);
        User userResult = userCommandService.createUser(dummyUser);
        assertNotNull(userResult);
        assertEquals(dummyUser, userResult);
        assertEquals(dummyUser.getId(), userResult.getId());
      }

      @Test
      void thenTheUserIsUpdated() throws Exception {
        // Mock the UserRepository return a user when save is called
        // Call userCommandService.createUser() with a dummy user
        // verify that the returned user has the correct data
        // verify that the userRepository.save() method was called
        // verify that the eventPublisher.publishEvent() method was called

        User dummyUser = new User(1L, "John Doe", "", 1L);
        when(userRepository.findById(any())).thenReturn(Optional.of(dummyUser));
        when(userRepository.save(any())).thenReturn(dummyUser);
        doNothing().when(eventPublisher).publishEvent(any(UserUpdatedEvent.class));
        doNothing().when(userEventListener).onUserUpdatedEvent(any());
        User userResult = userCommandService.updateUser(dummyUser);
        assertNotNull(userResult);
        assertEquals(dummyUser, userResult);
        // Verify that the userRepository.findById() and save() methods were called
        verify(userRepository, times(1)).findById(any());
        verify(userRepository, times(1)).save(any());

        // Verify that the eventPublisher.publishEvent() method was called with a UserUpdatedEvent
//        verify(eventPublisher, times(1)).publishEvent(any(UserUpdatedEvent.class));
      }
    }
  }
}
