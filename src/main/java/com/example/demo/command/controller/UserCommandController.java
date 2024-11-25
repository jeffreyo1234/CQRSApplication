package com.example.demo.command.controller;

import com.example.demo.command.model.User;
import com.example.demo.command.service.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// This controller has POST, PUT and DELETE methods to create, update and delete users.
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/command/users")
public class UserCommandController {

  private final UserCommandService userCommandService;

  // This method creates a new user
  @PostMapping
  public ResponseEntity<User> createUser(@RequestBody User user) {
    return ResponseEntity.ok(userCommandService.createUser(user));
  }

  // This method updates a user
  @PutMapping
  public ResponseEntity<User> updateUser(@RequestBody User user) {
    return ResponseEntity.ok(userCommandService.updateUser(user));
  }

  // This method deletes a user
  @DeleteMapping
  public ResponseEntity<Void> deleteUser(@RequestBody Long id) {
    userCommandService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}
