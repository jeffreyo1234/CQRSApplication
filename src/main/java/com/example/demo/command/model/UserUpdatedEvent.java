package com.example.demo.command.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserUpdatedEvent {
  private final Long id;
  private final String name;
  private final Long version;
}
