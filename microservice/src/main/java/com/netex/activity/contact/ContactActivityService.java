package com.netex.activity.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContactActivityService {

    private static final Logger log = LoggerFactory.getLogger(ContactActivityService.class);
    private final ContactActivityRepository repository;

    public ContactActivityService(ContactActivityRepository repository) {
        this.repository = repository;
    }

    public void record(ContactActivityRequest event) {
        if (repository.saveIfNew(event)) {
            log.info("Recorded {} activity for contact {}", event.action(), event.contactId());
        }
    }
}
