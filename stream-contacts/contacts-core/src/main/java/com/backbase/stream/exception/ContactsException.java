package com.backbase.stream.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ContactsException extends RuntimeException {

    private final String httpResponse;

    public ContactsException(WebClientResponseException throwable, String message) {
        super(message, throwable);
        this.httpResponse = throwable.getResponseBodyAsString();
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
