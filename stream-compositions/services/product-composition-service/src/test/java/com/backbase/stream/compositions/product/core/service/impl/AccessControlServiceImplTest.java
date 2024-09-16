package com.backbase.stream.compositions.product.core.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.dbs.accesscontrol.api.service.v3.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v3.ServiceAgreementsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.LegalEntityItem;
import com.backbase.dbs.accesscontrol.api.service.v3.model.ServiceAgreementItemQuery;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
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
    private LegalEntitiesApi legalEntitiesApi;
    @Mock
    private ServiceAgreementsApi serviceAgreementsApi;
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
        when(legalEntitiesApi.getMasterServiceAgreement(any()))
                .thenReturn(Mono.just(new ServiceAgreementItemQuery()
                        .id(SERVICE_AGREEMENT_ID)
                        .externalId(SERVICE_AGREEMENT_EXT_ID)));

        Mono<ServiceAgreementItemQuery> serviceAgreement = accessControlService
                .getMasterServiceAgreementByInternalLegalEntityId(SARA_LE_ID);

        StepVerifier.create(serviceAgreement)
                .assertNext(sa -> {
                    Assertions.assertEquals(SERVICE_AGREEMENT_ID, sa.getId());
                    Assertions.assertEquals(SERVICE_AGREEMENT_EXT_ID, sa.getExternalId());

                })
                .verifyComplete();
    }

    @Test
    void getMasterServiceAgreementByInternalLegalEntityId_error() {
        when(legalEntitiesApi.getMasterServiceAgreement(any()))
                .thenReturn(Mono.error(WebClientResponseException.NotFound
                        .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<ServiceAgreementItemQuery> serviceAgreement = accessControlService
                .getMasterServiceAgreementByInternalLegalEntityId(SARA_LE_ID);

        StepVerifier.create(serviceAgreement)
                .expectError()
                .verify();
    }

    @Test
    void getServiceAgreementById_success() {
        when(serviceAgreementsApi.getServiceAgreement(any()))
                .thenReturn(Mono.just(new ServiceAgreementItemQuery()
                        .id(SERVICE_AGREEMENT_ID)
                        .externalId(SERVICE_AGREEMENT_EXT_ID)));

        Mono<ServiceAgreementItemQuery> serviceAgreement = accessControlService
                .getServiceAgreementById(SERVICE_AGREEMENT_ID);

        StepVerifier.create(serviceAgreement)
                .assertNext(sa -> {
                    Assertions.assertEquals(SERVICE_AGREEMENT_ID, sa.getId());
                    Assertions.assertEquals(SERVICE_AGREEMENT_EXT_ID, sa.getExternalId());

                })
                .verifyComplete();
    }

    @Test
    void getServiceAgreementById_error() {
        when(serviceAgreementsApi.getServiceAgreement(any()))
                .thenReturn(Mono.error(WebClientResponseException.NotFound
                        .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<ServiceAgreementItemQuery> serviceAgreement = accessControlService
                .getServiceAgreementById(SERVICE_AGREEMENT_ID);

        StepVerifier.create(serviceAgreement)
                .expectError()
                .verify();
    }

    @Test
    void getLegalEntityById_success() {
        when(legalEntitiesApi.getLegalEntityById(any()))
                .thenReturn(Mono.just(new LegalEntityItem()
                        .id(SARA_LE_ID)
                        .externalId(SARA_LE_EXT_ID)));

        Mono<LegalEntityItem> legalEntity = accessControlService.getLegalEntityById(SARA_LE_ID);

        StepVerifier.create(legalEntity)
                .assertNext(le -> {
                    Assertions.assertEquals(SARA_LE_ID, le.getId());
                    Assertions.assertEquals(SARA_LE_EXT_ID, le.getExternalId());

                })
                .verifyComplete();

    }

    @Test
    void getLegalEntityById_error() {
        when(legalEntitiesApi.getLegalEntityById(any()))
                .thenReturn(Mono.error(WebClientResponseException.NotFound
                        .create(404, "not found", new HttpHeaders(), new byte[0], null)));

        Mono<LegalEntityItem> legalEntity = accessControlService.getLegalEntityById(SARA_LE_ID);

        StepVerifier.create(legalEntity)
                .expectError()
                .verify();

    }
}