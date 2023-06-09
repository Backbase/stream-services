package com.backbase.stream.exceptions;

import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class AccessGroupException extends RuntimeException {

    private final ServiceAgreement serviceAgreement;
    private String httpResponse;

    public AccessGroupException(
        WebClientResponseException throwable, ServiceAgreement serviceAgreement, String s) {
        super(s, throwable);
        this.serviceAgreement = serviceAgreement;
        this.httpResponse = throwable.getResponseBodyAsString();
    }

    public ServiceAgreement getServiceAgreement() {
        return serviceAgreement;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
