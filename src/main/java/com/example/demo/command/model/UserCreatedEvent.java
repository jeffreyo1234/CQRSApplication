package com.example.demo.command.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCreatedEvent {
  private final Long id;
  private final String name;
  private final String email;
  private final Long version;
}
