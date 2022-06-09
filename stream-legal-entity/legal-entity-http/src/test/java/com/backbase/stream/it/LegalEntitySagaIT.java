package com.backbase.stream.it;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.backbase.stream.LegalEntityHttpApplication;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(classes = LegalEntityHttpApplication.class)
@ContextConfiguration(classes = {LegalEntitySagaIT.TestConfiguration.class})
@TestPropertySource(properties = {"spring.config.location=classpath:application-it.yml"})
@AutoConfigureWebTestClient
public class LegalEntitySagaIT {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.client.provider.bb.token-uri",
                () -> String.format("%s/oauth/token", wireMockServer.baseUrl())
        );
        registry.add(
                "backbase.stream.dbs.access-control-base-url",
                () -> String.format("%s/access-control", wireMockServer.baseUrl())
        );
        registry.add(
                "backbase.stream.dbs.user-manager-base-url",
                () -> String.format("%s/user-manager", wireMockServer.baseUrl())
        );
        registry.add(
                "backbase.stream.dbs.arrangement-manager-base-url",
                () -> String.format("%s/arrangement-manager", wireMockServer.baseUrl())
        );
    }

    @Autowired
    private WebTestClient webTestClient;

    private static LegalEntityTask defaultLegalEntityTask() {
        return new LegalEntityTask()
                .data(
                        new LegalEntity()
                                .name("Legal Entity")
                                .externalId("100000")
                                .parentExternalId("parent-100000")
                                .legalEntityType(LegalEntityType.CUSTOMER)
                                .realmName("customer-bank")
                                .productGroups(Arrays.asList(
                                        (ProductGroup) new ProductGroup()
                                                .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
                                                .name("Default PG")
                                                .users(Collections.singletonList(
                                                        new JobProfileUser()
                                                                .user(
                                                                        new User()
                                                                                .externalId("john.doe")
                                                                                .fullName("John Doe")
                                                                                .identityLinkStrategy(IdentityUserLinkStrategy.IDENTITY_AGNOSTIC)
                                                                )
                                                                .referenceJobRoleNames(Collections.singletonList("Private - Read only"))
                                                ))
                                                .currentAccounts(Collections.singletonList(
                                                        (CurrentAccount) new CurrentAccount()
                                                                .BBAN("01318000")
                                                                .externalId("7155000")
                                                                .productTypeExternalId("privateCurrentAccount")
                                                                .name("Account 1")
                                                                .currency("GBP")
                                                )),
                                        (ProductGroup) new ProductGroup()
                                                .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
                                                .name("Mixed PG")
                                                .users(Collections.singletonList(
                                                        new JobProfileUser()
                                                                .user(
                                                                        new User()
                                                                                .externalId("john.doe")
                                                                                .fullName("John Doe")
                                                                                .identityLinkStrategy(IdentityUserLinkStrategy.IDENTITY_AGNOSTIC)
                                                                )
                                                                .referenceJobRoleNames(Collections.singletonList("Private - Full access"))
                                                ))
                                                .currentAccounts(Collections.singletonList(
                                                        (CurrentAccount) new CurrentAccount()
                                                                .BBAN("01318001")
                                                                .externalId("7155001")
                                                                .productTypeExternalId("privateCurrentAccount")
                                                                .name("Account 2")
                                                                .currency("GBP")
                                                ))
                                ))
                                .users(Collections.singletonList(
                                        new JobProfileUser()
                                                .user(
                                                        new User()
                                                                .externalId("john.doe")
                                                                .fullName("John Doe")
                                                                .identityLinkStrategy(IdentityUserLinkStrategy.CREATE_IN_IDENTITY)
                                                                .locked(false)
                                                                .emailAddress(new EmailAddress().address("test@example.com"))
                                                                .mobileNumber(new PhoneNumber().number("+12345"))
                                                )
                                                .referenceJobRoleNames(Arrays.asList(
                                                        "Private - Read only", "Private - Full access"
                                                ))
                                ))
                                .administrators(Collections.emptyList())
                                .customServiceAgreement(
                                        new ServiceAgreement()
                                                .externalId("Service_Agreement_Id")
                                                .name("Service Agreement")
                                                .description("Custom Service Agreement")
                                                .participants(Collections.singletonList(
                                                        new LegalEntityParticipant()
                                                                .externalId("user-external-id")
                                                                .sharingUsers(true)
                                                                .sharingAccounts(true)
                                                                .users(Collections.singletonList("john.doe"))
                                                ))
                                                .status(LegalEntityStatus.ENABLED)
                                                .isMaster(false)
                                )
                );
    }

    private void setupWireMock() {
        wireMockServer.stubFor(
                WireMock.post("/oauth/token")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"access_token\": \"access-token\",\n\"expires_in\": 600,\n\"refresh_expires_in\": 1800,\n\"refresh_token\": \"refresh-token\",\n\"token_type\": \"bearer\",\n\"id_token\": \"id-token\",\n\"not-before-policy\": 1633622545,\n\"session_state\": \"72a28739-3d20-4965-bd86-64410df53d04\",\n\"scope\": \"openid\"\n}"))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/legalentities/external/100000")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"type\":\"CUSTOMER\"\n}"))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/legalentities/500000")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"type\":\"CUSTOMER\"\n}"))
        );

        wireMockServer.stubFor(
                WireMock.put("/access-control/service-api/v2/legalentities")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"type\":\"CUSTOMER\"\n}"))
        );

        wireMockServer.stubFor(
                WireMock.get("/user-manager/service-api/v2/users/identities/realms")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"id\":\"0006f11c\",\"realmName\":\"customer-bank\"}]"))
        );

        wireMockServer.stubFor(
                WireMock.post("/user-manager/service-api/v2/users/identities/realms/customer-bank/legalentities")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        wireMockServer.stubFor(
                WireMock.get("/user-manager/service-api/v2/users/externalids/john.doe?skipHierarchyCheck=true")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"id\":\"9ac44fca\",\"externalId\":\"john.doe\",\"legalEntityId\":\"500000\",\"fullName\":\"John Doe\"}"))
        );

        wireMockServer.stubFor(
                WireMock.put("/user-manager/service-api/v2/users")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"id\":\"9ac44fca\",\"externalId\":\"john.doe\",\"legalEntityId\":\"500000\",\"fullName\":\"John Doe\"}"))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/accessgroups/serviceagreements/external/Service_Agreement_Id")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"additions\":{},\"creatorLegalEntity\":\"500000\",\"status\":\"ENABLED\",\"id\":\"500001\",\"externalId\":\"Service_Agreement_Id\",\"name\":\"\",\"description\":\"Custom Service Agreement\",\"isMaster\":false,\"validFromDate\":null,\"validFromTime\":null,\"validUntilDate\":null,\"validUntilTime\":null}"))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/accessgroups/service-agreements/500001/participants")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"sharingUsers\":true,\"sharingAccounts\":true}]"))
        );

        wireMockServer.stubFor(
                WireMock.put("/access-control/service-api/v2/accessgroups/serviceagreements/ingest/service-agreements/participants")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/accesscontrol/accessgroups/serviceagreements/500001/users")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.NOT_FOUND.value()))
        );

        wireMockServer.stubFor(
                WireMock.put("/access-control/service-api/v2/accessgroups/serviceagreements/ingest/service-agreements/users")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/accesscontrol/accessgroups/function-groups?serviceAgreementId=500001")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"additions\":{},\"id\":\"500002\",\"serviceAgreementId\":\"500001\",\"name\":\"Private - Read only\",\"description\":\"Private - Read only\",\"type\":\"TEMPLATE\",\"permissions\":[{\"functionId\":\"1029\",\"assignedPrivileges\":[{\"additions\":{},\"privilege\":\"view\"}]}]},{\"additions\":{},\"id\":\"500003\",\"serviceAgreementId\":\"500001\",\"name\":\"Private - Full access\",\"description\":\"Private - Full access\",\"type\":\"TEMPLATE\",\"permissions\":[{\"functionId\":\"1029\",\"assignedPrivileges\":[{\"additions\":{},\"privilege\":\"create\"},{\"additions\":{},\"privilege\":\"edit\"},{\"additions\":{},\"privilege\":\"delete\"},{\"additions\":{},\"privilege\":\"execute\"},{\"additions\":{},\"privilege\":\"view\"}]}]}]"))
        );

        wireMockServer.stubFor(
                WireMock.put("/access-control/service-api/v2/accessgroups/function-groups/batch/update")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        wireMockServer.stubFor(
                WireMock.put("/access-control/service-api/v2/accessgroups/users/permissions/user-permissions")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/accesscontrol/accessgroups/users/9ac44fca/service-agreements/500001/permissions")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"additions\":{},\"approvalId\":null,\"items\":[{\"additions\":{},\"functionGroupId\":\"500002\",\"dataGroupIds\":[],\"selfApprovalPolicies\":[]},{\"additions\":{},\"functionGroupId\":\"500003\",\"dataGroupIds\":[],\"selfApprovalPolicies\":[]}]}"))
        );

        wireMockServer.stubFor(
                WireMock.post("/arrangement-manager/service-api/v2/arrangements/batch")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        wireMockServer.stubFor(
                WireMock.get("/access-control/service-api/v2/accesscontrol/accessgroups/data-groups?serviceAgreementId=500001&includeItems=true")
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"additions\":{},\"id\":\"4002\",\"name\":\"Default PG\",\"description\":\"Default data group description\",\"serviceAgreementId\":\"500001\",\"type\":\"ARRANGEMENTS\",\"approvalId\":null,\"items\":[\"arrangement-id-1\"]},{\"additions\":{},\"id\":\"4003\",\"name\":\"Mixed PG\",\"description\":\"Default data group description\",\"serviceAgreementId\":\"500001\",\"type\":\"ARRANGEMENTS\",\"approvalId\":null,\"items\":[\"arrangement-id-2\"]}]"))
        );

        wireMockServer.stubFor(
                WireMock.put("/access-control/service-api/v2/accessgroups/data-groups/batch/update/data-items")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );
    }

    /**
     * Intention of this test is to verify that custom header which passed to legal entity controller is re-propagated
     * to downstream call as well.
     */
    @Test
    void legalEntitySaga() {
        // Given
        setupWireMock();
        LegalEntityTask legalEntityTask = defaultLegalEntityTask();

        // When
        webTestClient.mutateWith(csrf()).post()
                .uri("/legal-entity")
                .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
                .bodyValue(legalEntityTask.getLegalEntity())
                .exchange()
                .expectStatus().isEqualTo(200);

        // Then
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/access-control/service-api/v2/legalentities/500000"))
                .withHeader("X-TID", WireMock.equalTo("tenant-id")));

        Assertions.assertTrue(wireMockServer.findAllUnmatchedRequests().isEmpty());
    }
    @Test
    void legalEntitySagaUpdateLegalEntity() {
        // Given
        setupWireMock();
        LegalEntityTask legalEntityTask = defaultLegalEntityTask();

        // When
        webTestClient.mutateWith(csrf()).post()
                .uri("/legal-entity")
                .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
                .bodyValue(legalEntityTask.getLegalEntity())
                .exchange()
                .expectStatus().isEqualTo(200);

        webTestClient.mutateWith(csrf()).post()
                .uri("/legal-entity")
                .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
                .bodyValue(legalEntityTask.getLegalEntity().name("Updated name"))
                .exchange()
                .expectStatus().isEqualTo(200);


        // Then
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/access-control/service-api/v2/legalentities/500000"))
                .withHeader("X-TID", WireMock.equalTo("tenant-id")));

        Assertions.assertTrue(wireMockServer.findAllUnmatchedRequests().isEmpty());
    }

    public static class TestConfiguration {
        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

}
