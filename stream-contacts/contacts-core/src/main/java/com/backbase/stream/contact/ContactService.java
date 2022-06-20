package com.backbase.stream.contact;

import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.contact.api.service.v2.model.AccessContextScope;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostRequestBody;
import com.backbase.dbs.contact.api.service.v2.model.ContactsBulkPostResponseBody;
import com.backbase.dbs.contact.api.service.v2.model.ExternalAccessContext;
import com.backbase.stream.exception.ContactsException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class ContactService {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";
    private final ContactsApi contactsApi;

    /**
     * Post Bulk Contacts
     *
     * @param contactsBulkPostRequestBody The Request Body to ingest bulk contacts
     * @return The response from contacts post bulk request
     */
    public Mono<ContactsBulkPostResponseBody> postBulkContacts(ContactsBulkPostRequestBody contactsBulkPostRequestBody, String accessToken) {
        final String externalId = getContextType(contactsBulkPostRequestBody.getAccessContext());
        log.info("Contacts synching for {}", externalId);
        contactsApi.getApiClient().addDefaultHeader(AUTHORIZATION_HEADER, BEARER + accessToken);
        return contactsApi.postContactsBulk(contactsBulkPostRequestBody)
            .doOnError(WebClientResponseException.class, throwable ->
                    log.error("Failed to post bulk contacts for: {}\n{}", externalId, throwable.getResponseBodyAsString()))
            .onErrorMap(WebClientResponseException.class, throwable -> new ContactsException(throwable, "Failed to post bulk contacts"));
    }

    private String getContextType(ExternalAccessContext accessContext) {
        if (accessContext.getScope() == AccessContextScope.LE) {
            return accessContext.getExternalLegalEntityId();
        } else if (accessContext.getScope() == AccessContextScope.SA) {
            return accessContext.getExternalServiceAgreementId();
        } else if (accessContext.getScope() == AccessContextScope.USER) {
            return accessContext.getExternalUserId();
        }
        return null;
    }

}
