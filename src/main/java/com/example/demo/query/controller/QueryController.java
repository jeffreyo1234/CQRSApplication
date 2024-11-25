package com.example.demo.query.controller;

import com.example.demo.query.model.UserProjection;
import com.example.demo.query.service.QueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// This controller will have methods to query, read the data
// This is the query side of the CQRS pattern
@RestController
@RequestMapping("/api/query/users")
@RequiredArgsConstructor
public class QueryController {

  private final QueryService queryService;

  // This method returns all users
  @GetMapping
  public ResponseEntity<List<UserProjection>> getAllUsers() {
    return ResponseEntity.ok(queryService.getAllUsers());
  }

  // This method returns a user by id
  @GetMapping("/{id}")
  public ResponseEntity<UserProjection> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(queryService.getUserById(id));
  }
}
