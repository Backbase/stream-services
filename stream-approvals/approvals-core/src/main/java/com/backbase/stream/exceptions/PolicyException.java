package com.backbase.stream.exceptions;

import com.backbase.stream.approval.model.Policy;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class PolicyException extends RuntimeException {

    private final Policy policy;
    private final String message;
    private final String httpResponse;

    public PolicyException(Policy policy, String message, WebClientResponseException exception) {
        super(exception);
        httpResponse = exception.getResponseBodyAsString();
        this.policy = policy;
        this.message = message;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
