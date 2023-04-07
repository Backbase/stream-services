package com.backbase.stream.exceptions;

import com.backbase.stream.approval.model.ApprovalType;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ApprovalTypeException extends RuntimeException {

    private final ApprovalType approvalType;
    private final String message;
    private final String httpResponse;

    public ApprovalTypeException(
            ApprovalType approvalType, String message, WebClientResponseException exception) {
        super(exception);
        httpResponse = exception.getResponseBodyAsString();
        this.approvalType = approvalType;
        this.message = message;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
