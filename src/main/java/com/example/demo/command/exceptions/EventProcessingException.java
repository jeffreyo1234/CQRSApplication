package com.example.demo.command.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EventProcessingException extends RuntimeException {
  public EventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
