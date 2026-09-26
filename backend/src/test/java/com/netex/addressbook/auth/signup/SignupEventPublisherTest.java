package com.netex.addressbook.auth.signup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignupEventPublisherTest {

    @Test
    void publishesOnlyPublicSignupFields() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        when(kafka.send(eq(SignupEventPublisher.TOPIC), eq("42"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        new SignupEventPublisher(kafka, json).publish(42, "ana@example.com");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("user-signups"), eq("42"), message.capture());
        JsonNode event = json.readTree(message.getValue());
        assertThat(event.get("userId").asLong()).isEqualTo(42);
        assertThat(event.get("email").asText()).isEqualTo("ana@example.com");
        assertThat(event.hasNonNull("signedUpAt")).isTrue();
        assertThat(event.size()).isEqualTo(3);
    }
}
