package com.backbase.stream.compositions.product.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.legalentity.api.service.v1.LegalEntityApi;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.LegalEntityWithParent;
import com.backbase.accesscontrol.legalentity.api.service.v1.model.SingleServiceAgreement;
import com.backbase.accesscontrol.serviceagreement.api.service.v1.ServiceAgreementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {

    private static final String SARA_EXT_ID = "sara";
    private static final String SARA_LE_ID = "sara_le";
    private static final String SARA_LE_EXT_ID = "sara";
    private static final String SARA_ID = "123212312";
    private static final String SERVICE_AGREEMENT_ID = "123212312";
    private static final String SERVICE_AGREEMENT_EXT_ID = "sara_sa";
    @Mock
    private UserManagementApi userManagementApi;
    @Mock
    private LegalEntityApi legalEntitiesApi;
    @Mock
    private ServiceAgreementApi serviceAgreementsApi;
    @InjectMocks
    private AccessControlServiceImpl accessControlService;

    @Test
    void getUserByExternalId_success() {
        when(userManagementApi.getUserByExternalId(any(), any()))
            .thenReturn(Mono.just(new GetUser().externalId(SARA_EXT_ID).id(SARA_ID)));

        Mono<GetUser> responseMono = accessControlService.getUserByExternalId(SARA_EXT_ID, false);

        StepVerifier.create(responseMono)
            .assertNext(user -> Assertions.assertEquals(SARA_ID, user.getId()))
            .verifyComplete();
    }

    @Test
    void getUserByExternalId_error() {
        when(userManagementApi.getUserByExternalId(any(), any()))
            .thenReturn(Mono.error(WebClientResponseException.NotFound
                .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<GetUser> userByExternalId = accessControlService.getUserByExternalId(SARA_EXT_ID, false);

        StepVerifier.create(userByExternalId)
            .expectError()
            .verify();
    }

    @Test
    void getMasterServiceAgreementByInternalLegalEntityId_success() {
        when(legalEntitiesApi.getSingleServiceAgreement(any()))
            .thenReturn(Mono.just(new SingleServiceAgreement()
                .id(SERVICE_AGREEMENT_ID)
                .externalId(SERVICE_AGREEMENT_EXT_ID)));

        Mono<ServiceAgreement> serviceAgreement = accessControlService
            .getMasterServiceAgreementByInternalLegalEntityId(SARA_LE_ID);

        StepVerifier.create(serviceAgreement)
            .assertNext(sa -> {
                Assertions.assertEquals(SERVICE_AGREEMENT_ID, sa.getInternalId());
                Assertions.assertEquals(SERVICE_AGREEMENT_EXT_ID, sa.getExternalId());

            })
            .verifyComplete();
    }

    @Test
    void getMasterServiceAgreementByInternalLegalEntityId_error() {
        when(legalEntitiesApi.getSingleServiceAgreement(any()))
            .thenReturn(Mono.error(WebClientResponseException.NotFound
                .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<ServiceAgreement> serviceAgreement = accessControlService
            .getMasterServiceAgreementByInternalLegalEntityId(SARA_LE_ID);

        StepVerifier.create(serviceAgreement)
            .expectError()
            .verify();
    }

    @Test
    void getServiceAgreementById_success() {
        when(serviceAgreementsApi.getServiceAgreementById(any()))
            .thenReturn(
                Mono.just(new com.backbase.accesscontrol.serviceagreement.api.service.v1.model.ServiceAgreement()
                    .id(SERVICE_AGREEMENT_ID)
                    .externalId(SERVICE_AGREEMENT_EXT_ID)));

        Mono<ServiceAgreement> serviceAgreement = accessControlService
            .getServiceAgreementById(SERVICE_AGREEMENT_ID);

        StepVerifier.create(serviceAgreement)
            .assertNext(sa -> {
                Assertions.assertEquals(SERVICE_AGREEMENT_ID, sa.getInternalId());
                Assertions.assertEquals(SERVICE_AGREEMENT_EXT_ID, sa.getExternalId());

            })
            .verifyComplete();
    }

    @Test
    void getServiceAgreementById_error() {
        when(serviceAgreementsApi.getServiceAgreementById(any()))
            .thenReturn(Mono.error(WebClientResponseException.NotFound
                .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<ServiceAgreement> serviceAgreement = accessControlService
            .getServiceAgreementById(SERVICE_AGREEMENT_ID);

        StepVerifier.create(serviceAgreement)
            .expectError()
            .verify();
    }

    @Test
    void getLegalEntityById_success() {
        when(legalEntitiesApi.getLegalEntityById(any()))
            .thenReturn(Mono.just(new LegalEntityWithParent()
                .id(SARA_LE_ID)
                .externalId(SARA_LE_EXT_ID)));

        Mono<LegalEntity> legalEntity = accessControlService.getLegalEntityById(SARA_LE_ID);

        StepVerifier.create(legalEntity)
            .assertNext(le -> {
                Assertions.assertEquals(SARA_LE_ID, le.getInternalId());
                Assertions.assertEquals(SARA_LE_EXT_ID, le.getExternalId());

            })
            .verifyComplete();

    }

    @Test
    void getLegalEntityById_error() {
        when(legalEntitiesApi.getLegalEntityById(any()))
            .thenReturn(Mono.error(WebClientResponseException.NotFound
                .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<LegalEntity> legalEntity = accessControlService.getLegalEntityById(SARA_LE_ID);

        StepVerifier.create(legalEntity)
            .expectError()
            .verify();

    }
}