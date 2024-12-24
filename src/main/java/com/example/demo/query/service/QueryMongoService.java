package com.example.demo.query.service;

import com.example.demo.query.model.UserMongoProjection;
import com.example.demo.query.model.UserProjection;
import com.example.demo.query.repo.QueryMongoRepository;
import com.example.demo.query.repo.QueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class QueryMongoService {
  private final QueryMongoRepository queryMongoRepository;

  public UserMongoProjection getUserById(Long id) {
    return queryMongoRepository.findById(id).orElse(null);
  }

  public List<UserMongoProjection> getAllUsers() {
    return queryMongoRepository.findAll();
  }
}
