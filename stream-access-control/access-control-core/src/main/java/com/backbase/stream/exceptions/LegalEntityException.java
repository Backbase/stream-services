package com.backbase.stream.exceptions;

import com.backbase.dbs.accesscontrol.api.service.v2.model.LegalEntityCreateItem;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class LegalEntityException extends RuntimeException {

    LegalEntityCreateItem legalEntityCreateItem;
    String httpResponse;

    public LegalEntityException(LegalEntityCreateItem legalEntity, String message,
        WebClientResponseException exception) {
        super(exception);
        httpResponse = exception.getResponseBodyAsString();
        this.legalEntityCreateItem = legalEntity;
    }

    public String getHttpResponse() {
        return httpResponse;
    }
}
