package com.backbase.stream.product.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ArrangementUpdateException extends RuntimeException {
    private final String httpResponse;

    public ArrangementUpdateException(WebClientResponseException throwable, String message) {
        super(message, throwable);
        this.httpResponse = throwable.getResponseBodyAsString();
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
