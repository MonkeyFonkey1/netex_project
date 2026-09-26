package com.netex.addressbook.contact;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSearchTermException extends RuntimeException {

    public InvalidSearchTermException(String message) {
        super(message);
    }
}
