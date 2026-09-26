package com.netex.addressbook.activity;

import com.netex.addressbook.auth.signup.SignupEventPublisher;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", matchIfMissing = true)
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final EventOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final RestClient activityHttp;

    public OutboxDispatcher(EventOutboxRepository outbox, KafkaTemplate<String, String> kafka,
            RestClient.Builder builder, @Value("${app.activity-service.url}") String serviceUrl) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.activityHttp = builder.baseUrl(serviceUrl).build();
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
    public void deliverDueEvents() {
        for (EventOutboxRepository.PendingEvent event : outbox.due()) {
            try {
                switch (event.destination()) {
                    case "KAFKA_SIGNUP" -> kafka.send(SignupEventPublisher.TOPIC,
                            event.eventKey(), event.payload()).get(3, TimeUnit.SECONDS);
                    case "HTTP_CONTACT" -> activityHttp.post().uri("/internal/contact-events")
                            .header("Content-Type", "application/json")
                            .body(event.payload()).retrieve().toBodilessEntity();
                    default -> throw new IllegalStateException("Unknown outbox destination " + event.destination());
                }
                outbox.delivered(event.id());
            } catch (Exception exception) {
                log.warn("Event {} could not be delivered; it will be retried", event.id(), exception);
                outbox.retryLater(event.id());
            }
        }
    }
}
