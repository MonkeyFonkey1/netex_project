package com.netex.addressbook.contact;

import com.netex.addressbook.activity.ContactActivityClient;
import com.netex.addressbook.auth.AppUser;
import org.springframework.stereotype.Service;

@Service
public class ContactCommandService {

    private final ContactService contacts;
    private final ContactActivityClient activity;

    public ContactCommandService(ContactService contacts, ContactActivityClient activity) {
        this.contacts = contacts;
        this.activity = activity;
    }

    public Contact create(ContactRequest request, long authorId) {
        Contact contact = contacts.create(request, authorId);
        activity.created(contact.id(), authorId);
        return contact;
    }

    public Contact update(long id, ContactRequest request, AppUser user) {
        Contact contact = contacts.update(id, request, user);
        activity.updated(id, user.id());
        return contact;
    }

    public void delete(long id, AppUser user) {
        contacts.delete(id, user);
        activity.deleted(id, user.id());
    }
}
