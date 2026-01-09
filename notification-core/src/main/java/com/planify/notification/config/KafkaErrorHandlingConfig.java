package com.planify.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;

@Configuration
@Profile("!azure")
@Slf4j
public class KafkaErrorHandlingConfig {

    @Bean
    public KafkaListenerErrorHandler kafkaListenerErrorHandler() {
        return (message, exception) -> {
            log.error("Error processing Kafka message: {}", message.getPayload());
            
            if (exception instanceof ListenerExecutionFailedException) {
                ListenerExecutionFailedException listenerException = (ListenerExecutionFailedException) exception;
                log.error("Listener execution failed: {}", listenerException.getMessage());
                
                // Preverimo ali je prišlo do napake pri konverziji tipov
                if (listenerException.getCause() != null) {
                    Throwable cause = listenerException.getCause();
                    if (cause.getMessage() != null && cause.getMessage().contains("Cannot convert from")) {
                        log.error("Type conversion error detected. Message sent to wrong topic or with wrong type.");
                        log.error("Message headers: {}", ((Message<?>) message).getHeaders());
                        log.error("Expected a different event type. Skipping this message.");
                        return null;
                    }
                }
            }
            
            log.error("Unhandled exception type", exception);
            throw exception;
        };
    }
}
