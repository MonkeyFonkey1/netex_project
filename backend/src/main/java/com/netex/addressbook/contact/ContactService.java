package com.netex.addressbook.contact;

import com.netex.addressbook.auth.AppUser;
import com.netex.addressbook.image.PictureStorage;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactService {

    private final ContactRepository repository;
    private final PictureStorage pictureStorage;

    public ContactService(ContactRepository repository, PictureStorage pictureStorage) {
        this.repository = repository;
        this.pictureStorage = pictureStorage;
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

    @Transactional
    public Contact create(ContactRequest request, long authorId) {
        long id = repository.create(request.name().strip(), request.address().strip(), authorId);
        return findExisting(id);
    }

    @Transactional
    public Contact update(long id, ContactRequest request, AppUser user) {
        requireManager(id, user);
        int changed = repository.update(id, user.id(), user.isAdmin(),
                request.name().strip(), request.address().strip());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found");
        }
        return findExisting(id);
    }

    public void delete(long id, AppUser user) {
        requireManager(id, user);
        Contact deleted = repository.delete(id, user.id(), user.isAdmin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        pictureStorage.deleteIfExists(deleted.picturePath());
    }

    private Contact findExisting(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
    }

    public Contact requireManager(long id, AppUser user) {
        Contact contact = findExisting(id);
        if (contact.createdByUserId() != user.id() && !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the author or an admin can manage this contact");
        }
        return contact;
    }
}
