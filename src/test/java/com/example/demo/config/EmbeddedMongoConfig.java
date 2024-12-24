package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.demo.query.repo")
public class EmbeddedMongoConfig {

  @Bean
  public MongoTemplate mongoTemplate() {
    return new MongoTemplate(
        new SimpleMongoClientDatabaseFactory("mongodb://localhost:27017/test"));
  }
}
