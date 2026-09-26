package com.netex.addressbook.image;

import com.netex.addressbook.auth.AppUser;
import com.netex.addressbook.contact.Contact;
import com.netex.addressbook.contact.ContactResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contacts/{id}/picture")
public class ContactPictureController {

    private final ContactPictureService service;

    public ContactPictureController(ContactPictureService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<byte[]> read(@PathVariable long id) {
        ContactPictureService.PictureContent picture = service.read(id);
        return ResponseEntity.ok()
                .contentType(picture.contentType())
                .cacheControl(CacheControl.noStore())
                .body(picture.bytes());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContactResponse replace(@PathVariable long id, @RequestPart("picture") MultipartFile picture,
            @AuthenticationPrincipal AppUser user) {
        Contact contact = service.replace(id, user, picture);
        return ContactResponse.from(contact, user);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable long id, @AuthenticationPrincipal AppUser user) {
        service.remove(id, user);
    }
}
