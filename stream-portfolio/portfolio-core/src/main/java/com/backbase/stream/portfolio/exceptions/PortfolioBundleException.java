package com.backbase.stream.portfolio.exceptions;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class PortfolioBundleException extends RuntimeException {

    private final Object object;
    private final String message;
    private final String httpResponse;

    public PortfolioBundleException(
        Object object, String message, WebClientResponseException exception) {
        super(exception);
        httpResponse = exception.getResponseBodyAsString();
        this.object = object;
        this.message = message;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
