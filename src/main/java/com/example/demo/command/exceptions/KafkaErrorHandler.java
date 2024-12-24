package com.example.demo.command.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaErrorHandler implements KafkaListenerErrorHandler {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public KafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
    log.error(
        "Error Handler caught exception: {} for message: {}",
        exception.getMessage(),
        message,
        exception);

    sendToDLQ(message, exception);

    // You could implement custom retry logic here
    // For example, send to a DLQ topic
    return null;
  }

  private void sendToDLQ(Message<?> message, ListenerExecutionFailedException exception) {
    log.info("Sending message to DLQ: {}", message);
    try {
      kafkaTemplate.send("dlq-topic", message.getPayload()).get();
      log.info("Message successfully sent to DLQ");
    } catch (Exception e) {
      log.error("Failed to send message to DLQ: {}", message, e);
    }
  }
}
