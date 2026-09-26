package com.netex.addressbook.contact;

import com.netex.addressbook.activity.ContactActivityPublisher;
import com.netex.addressbook.auth.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactCommandService {

    private final ContactService contacts;
    private final ContactActivityPublisher activity;

    public ContactCommandService(ContactService contacts, ContactActivityPublisher activity) {
        this.contacts = contacts;
        this.activity = activity;
    }

    @Transactional
    public Contact create(ContactRequest request, long authorId) {
        Contact contact = contacts.create(request, authorId);
        activity.created(contact.id(), authorId);
        return contact;
    }

    @Transactional
    public Contact update(long id, ContactRequest request, AppUser user) {
        Contact contact = contacts.update(id, request, user);
        activity.updated(id, user.id());
        return contact;
    }

    @Transactional
    public void delete(long id, AppUser user) {
        contacts.delete(id, user);
        activity.deleted(id, user.id());
    }
}
