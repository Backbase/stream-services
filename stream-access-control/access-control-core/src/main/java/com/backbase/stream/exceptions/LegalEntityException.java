package com.backbase.stream.exceptions;

import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Getter
public class LegalEntityException extends RuntimeException {

    private final String httpResponse;

    public LegalEntityException(WebClientResponseException exception) {
        super(exception);
        httpResponse = exception.getResponseBodyAsString();
    }
}
