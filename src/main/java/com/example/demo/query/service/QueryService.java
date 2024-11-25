package com.example.demo.query.service;

import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// This service will have methods to query, read the data
// This is the query side of the CQRS pattern
@RequiredArgsConstructor
@Service
public class QueryService {

  private final QueryRepository queryRepository;

  public UserProjection getUserById(Long id) {
    return queryRepository.findById(id).orElse(null);
  }

  public List<UserProjection> getAllUsers() {
    return queryRepository.findAll();
  }
}
