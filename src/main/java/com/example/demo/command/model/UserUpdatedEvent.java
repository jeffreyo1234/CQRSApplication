package com.example.demo.command.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdatedEvent extends UserEvent {
  private String name;

  public UserUpdatedEvent(Long id, String name, Long version) {
    this.id = id;
    this.name = name;
    this.version = version;
  }
}