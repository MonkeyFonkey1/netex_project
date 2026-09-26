package com.netex.addressbook.contact;

import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts/export")
public class ContactExportController {

    private final ContactService contacts;
    private final ContactCsvService csv;

    public ContactExportController(ContactService contacts, ContactCsvService csv) {
        this.contacts = contacts;
        this.csv = csv;
    }

    @GetMapping
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String name) {
        List<Contact> matchingContacts = contacts.list(name);
        byte[] file = csv.export(matchingContacts);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contacts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .cacheControl(CacheControl.noStore())
                .body(file);
    }
}
