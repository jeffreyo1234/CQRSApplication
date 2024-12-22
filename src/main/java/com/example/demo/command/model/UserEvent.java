package com.example.demo.command.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = UserCreatedEvent.class, name = "UserCreatedEvent"),
  @JsonSubTypes.Type(value = UserUpdatedEvent.class, name = "UserUpdatedEvent")
})
@Getter
@Setter
@NoArgsConstructor
public abstract class UserEvent {
  protected Long id;
  protected Long version;
}
