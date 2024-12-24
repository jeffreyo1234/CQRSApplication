package com.example.demo.query.repo;

import com.example.demo.query.model.UserMongoProjection;
import com.example.demo.query.model.UserProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryMongoRepository extends MongoRepository<UserMongoProjection, Long> {}
