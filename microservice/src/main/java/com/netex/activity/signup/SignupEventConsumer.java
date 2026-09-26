package com.netex.activity.signup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SignupEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SignupEventConsumer.class);
    private final ObjectMapper json;
    private final SignupEventRepository repository;

    public SignupEventConsumer(ObjectMapper json, SignupEventRepository repository) {
        this.json = json;
        this.repository = repository;
    }

    @KafkaListener(topics = "user-signups")
    public void consume(String message) throws JsonProcessingException {
        UserSignedUpEvent event = json.readValue(message, UserSignedUpEvent.class);
        if (event.userId() <= 0 || event.email() == null || event.email().isBlank()
                || event.signedUpAt() == null) {
            throw new IllegalArgumentException("Invalid signup event");
        }
        if (repository.saveIfNew(event)) {
            log.info("Processed signup event for user {}", event.userId());
        }
    }
}
