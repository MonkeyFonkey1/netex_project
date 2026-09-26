package com.netex.addressbook.auth.signup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netex.addressbook.activity.EventOutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SignupEventPublisherTest {

    @Test
    void publishesOnlyPublicSignupFields() throws Exception {
        EventOutboxRepository outbox = mock(EventOutboxRepository.class);
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

        new SignupEventPublisher(outbox, json).publish(42, "ana@example.com");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(outbox).enqueue(eq("KAFKA_SIGNUP"), eq("42"), message.capture());
        JsonNode event = json.readTree(message.getValue());
        assertThat(event.get("userId").asLong()).isEqualTo(42);
        assertThat(event.get("email").asText()).isEqualTo("ana@example.com");
        assertThat(event.hasNonNull("signedUpAt")).isTrue();
        assertThat(event.size()).isEqualTo(3);
    }
}
