package com.example.demo.listener;

import com.example.demo.command.model.UserCreatedEvent;
import com.example.demo.command.model.UserUpdatedEvent;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// This class will listen to the events and perform the necessary actions
@Component
@RequiredArgsConstructor
public class UserEventListener {

  private final QueryRepository queryRepository;

  // This method will be called when a UserCreatedEvent is published
  // if the event user is not null, it will be saved to the queryRepository
  @EventListener
  public void onUserCreatedEvent(UserCreatedEvent userCreatedEvent) {

    if (!queryRepository.existsById(userCreatedEvent.getId())) {
      queryRepository.save(
          new UserProjection(
              userCreatedEvent.getId(),
              userCreatedEvent.getName(),
              userCreatedEvent.getEmail(),
              userCreatedEvent.getVersion()));
    }
    System.out.println("User created event received: " + userCreatedEvent);
  }

  // This method will be called when a UserUpdatedEvent is published
  // find the user by id and update the name if the version from the event is greater than the
  // version in the repository
  @EventListener
  public void onUserUpdatedEvent(UserUpdatedEvent userUpdatedEvent) {
    UserProjection userProjection =
        queryRepository
            .findById(userUpdatedEvent.getId())
            .orElseThrow(() -> new IllegalArgumentException("User Projection not found"));
    if (userUpdatedEvent.getVersion() > userProjection.getVersion()) {
      userProjection.setName(userUpdatedEvent.getName());
      userProjection.setVersion(userUpdatedEvent.getVersion());
      userProjection.setEmail(userProjection.getEmail());
      queryRepository.save(userProjection);
    }
  }
}
