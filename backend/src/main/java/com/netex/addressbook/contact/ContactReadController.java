package com.netex.addressbook.contact;

import com.netex.addressbook.auth.AppUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactReadController {

    private final ContactService service;

    public ContactReadController(ContactService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContactResponse> list(@RequestParam(required = false) String name,
            @AuthenticationPrincipal AppUser user) {
        List<ContactResponse> responses = new ArrayList<>();
        for (Contact contact : service.list(name)) {
            responses.add(ContactResponse.from(contact, user));
        }
        return responses;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> findById(@PathVariable long id,
            @AuthenticationPrincipal AppUser user) {
        Optional<Contact> result = service.findById(id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ContactResponse.from(result.get(), user));
    }
}
