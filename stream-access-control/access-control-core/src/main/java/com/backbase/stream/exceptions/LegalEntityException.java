package com.backbase.stream.exceptions;

import com.backbase.accesscontrol.legalentity.api.integration.v3.model.LegalEntityItem;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class LegalEntityException extends RuntimeException {

    LegalEntityItem legalEntityCreateItem;
    String httpResponse;

    public LegalEntityException(LegalEntityItem legalEntity, String message,
        WebClientResponseException exception) {
        super(exception);
        httpResponse = exception.getResponseBodyAsString();
        this.legalEntityCreateItem = legalEntity;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
