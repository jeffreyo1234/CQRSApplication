package com.example.demo.command.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  @Bean
  public NewTopic userEventsTopic() {
    return new NewTopic("user-events", 1, (short) 1); // Topic: user-events
  }
}
