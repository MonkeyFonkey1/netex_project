package com.netex.addressbook.contact;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final ContactRepository repository;

    public ContactService(ContactRepository repository) {
        this.repository = repository;
    }

    public List<Contact> list(String name) {
        String search = name == null ? "" : name.strip();
        if (search.length() > 255) {
            throw new InvalidSearchTermException("Name search must be at most 255 characters.");
        }
        return repository.findAll(search);
    }

    public Optional<Contact> findById(long id) {
        return repository.findById(id);
    }
}
