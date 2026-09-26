package com.netex.addressbook.activity;

import com.netex.addressbook.PostgresTestConfiguration;
import com.netex.addressbook.contact.ContactCommandService;
import com.netex.addressbook.contact.ContactRequest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@SpringBootTest(properties = "app.outbox.enabled=false")
@Import(PostgresTestConfiguration.class)
@Transactional
class OutboxDeliveryTest {

    @Autowired EventOutboxRepository outbox;
    @Autowired JdbcTemplate jdbc;
    @Autowired ContactCommandService contacts;

    @Test
    void contactCreationAndItsEventAreSavedTogether() {
        jdbc.update("DELETE FROM event_outbox");
        long userId = jdbc.queryForObject("""
                INSERT INTO users (email, password_hash) VALUES (?, 'test-hash') RETURNING id
                """, Long.class, UUID.randomUUID() + "@example.com");

        long contactId = contacts.create(new ContactRequest("Maria", "Strada 1"), userId).id();

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM event_outbox
                WHERE destination = 'HTTP_CONTACT' AND payload LIKE ?
                """, Integer.class, "%\"contactId\":" + contactId + "%")).isEqualTo(1);
    }

    @Test
    void retriesFailedHttpDeliveryWithoutLosingTheEvent() {
        jdbc.update("DELETE FROM event_outbox");
        outbox.enqueue("HTTP_CONTACT", "event-1", "{\"eventId\":\"event-1\"}");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8081/internal/contact-events"))
                .andRespond(withServerError());
        server.expect(requestTo("http://localhost:8081/internal/contact-events"))
                .andRespond(withNoContent());
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        OutboxDispatcher dispatcher = new OutboxDispatcher(outbox, kafka, builder, "http://localhost:8081");

        dispatcher.deliverDueEvents();
        assertThat(jdbc.queryForObject("SELECT attempts FROM event_outbox", Integer.class)).isEqualTo(1);
        jdbc.update("UPDATE event_outbox SET next_attempt_at = CURRENT_TIMESTAMP");
        dispatcher.deliverDueEvents();

        server.verify();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_outbox", Integer.class)).isZero();
    }

    @Test
    void retriesFailedKafkaDeliveryWithoutLosingTheEvent() {
        jdbc.update("DELETE FROM event_outbox");
        outbox.enqueue("KAFKA_SIGNUP", "42", "{\"userId\":42}");
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
                .thenReturn(CompletableFuture.completedFuture(null));
        OutboxDispatcher dispatcher = new OutboxDispatcher(outbox, kafka, RestClient.builder(),
                "http://localhost:8081");

        dispatcher.deliverDueEvents();
        assertThat(jdbc.queryForObject("SELECT attempts FROM event_outbox", Integer.class)).isEqualTo(1);
        jdbc.update("UPDATE event_outbox SET next_attempt_at = CURRENT_TIMESTAMP");
        dispatcher.deliverDueEvents();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_outbox", Integer.class)).isZero();
    }
}
