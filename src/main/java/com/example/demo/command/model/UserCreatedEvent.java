package com.example.demo.command.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserCreatedEvent extends UserEvent {
  private Long id;
  private String name;
  private String email;
  private Long version;


  // Parameterized constructor
  public UserCreatedEvent(Long id, String name, String email, Long version) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.version = version;
  }

  // Getters and setters
  // ...
}
