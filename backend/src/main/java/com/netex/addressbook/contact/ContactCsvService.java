package com.netex.addressbook.contact;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContactCsvService {

    public byte[] export(List<Contact> contacts) {
        // The BOM helps spreadsheet applications on Windows recognise UTF-8 names and addresses.
        StringBuilder csv = new StringBuilder("\uFEFFname,address,picture_url\r\n");
        for (Contact contact : contacts) {
            csv.append(field(contact.name())).append(',')
                    .append(field(contact.address())).append(',')
                    .append(field(ContactResponse.pictureUrlFor(contact)))
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String field(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
