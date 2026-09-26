package com.netex.addressbook.contact;

import com.netex.addressbook.auth.AppUser;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactWriteController {

    private final ContactCommandService service;

    public ContactWriteController(ContactCommandService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal AppUser user) {
        Contact contact = service.create(request, user.id());
        return ResponseEntity.created(URI.create("/api/contacts/" + contact.id()))
                .body(ContactResponse.from(contact, user));
    }

    @PutMapping("/{id}")
    public ContactResponse update(@PathVariable long id, @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal AppUser user) {
        Contact contact = service.update(id, request, user);
        return ContactResponse.from(contact, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @AuthenticationPrincipal AppUser user) {
        service.delete(id, user);
    }
}
