package com.netex.addressbook.image;

import com.netex.addressbook.activity.ContactActivityPublisher;
import com.netex.addressbook.auth.AppUser;
import com.netex.addressbook.contact.Contact;
import com.netex.addressbook.contact.ContactRepository;
import com.netex.addressbook.contact.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactPictureService {

    private final ContactService contacts;
    private final ContactRepository repository;
    private final PictureStorage storage;
    private final ContactActivityPublisher activity;

    public ContactPictureService(ContactService contacts, ContactRepository repository,
            PictureStorage storage, ContactActivityPublisher activity) {
        this.contacts = contacts;
        this.repository = repository;
        this.storage = storage;
        this.activity = activity;
    }

    @Transactional
    public Contact replace(long contactId, AppUser user, MultipartFile file) {
        Contact previous = contacts.requireManager(contactId, user);
        String newFilename = storage.store(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storage.deleteIfExists(newFilename);
            }
        });
        updatePath(contactId, user, previous.picturePath(), newFilename);
        activity.updated(contactId, user.id());
        deleteOldPictureAfterCommit(previous.picturePath());
        return findExisting(contactId);
    }

    @Transactional
    public void remove(long contactId, AppUser user) {
        Contact previous = contacts.requireManager(contactId, user);
        if (previous.picturePath() == null) {
            return;
        }
        updatePath(contactId, user, previous.picturePath(), null);
        activity.updated(contactId, user.id());
        deleteOldPictureAfterCommit(previous.picturePath());
    }

    public PictureContent read(long contactId) {
        Contact contact = findExisting(contactId);
        if (contact.picturePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Picture not found");
        }
        String filename = contact.picturePath();
        return new PictureContent(storage.read(filename), storage.contentType(filename));
    }

    private void updatePath(long id, AppUser user, String oldPath, String newPath) {
        if (repository.updatePicture(id, user.id(), user.isAdmin(), oldPath, newPath) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Picture changed; refresh the contact");
        }
    }

    private Contact findExisting(long id) {
        return contacts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
    }

    private void deleteOldPictureAfterCommit(String filename) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.deleteIfExists(filename);
            }
        });
    }

    public record PictureContent(byte[] bytes, MediaType contentType) {
    }
}
