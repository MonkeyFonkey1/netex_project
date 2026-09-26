package com.netex.addressbook.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ActivityHistoryClient {

    private static final Logger log = LoggerFactory.getLogger(ActivityHistoryClient.class);
    private final RestClient http;

    public ActivityHistoryClient(RestClient.Builder builder,
            @Value("${app.activity-service.url}") String serviceUrl) {
        this.http = builder.baseUrl(serviceUrl).build();
    }

    public ActivityHistoryResponse recent() {
        try {
            ActivityHistoryResponse history = http.get().uri("/internal/activity")
                    .retrieve().body(ActivityHistoryResponse.class);
            if (history == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Activity service returned no data");
            }
            return history;
        } catch (RestClientException exception) {
            log.error("Could not load activity history", exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Activity history is temporarily unavailable");
        }
    }
}
