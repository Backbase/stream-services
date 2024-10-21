package com.backbase.stream.it;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionLimit;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.LegalEntityStatus;
import com.backbase.stream.legalentity.model.LegalEntityType;
import com.backbase.stream.legalentity.model.Limit;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.PhoneNumber;
import com.backbase.stream.legalentity.model.Privilege;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@ActiveProfiles("it")
@WireMockTest(httpPort = 10000)
@AutoConfigureWebTestClient(timeout = "20000")
class LegalEntitySagaIT {

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("management.tracing.enabled", () -> true);
        registry.add("management.tracing.propagation.type", () -> "B3_MULTI");
        registry.add("management.zipkin.tracing.endpoint", () -> "http://localhost:10000/api/v2/spans");
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
                    .limit(limitWithBussinessFunctionAndPrivileges())
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
                            .loans(List.of(new Loan().IBAN("IBAN")))
                    ))
                    .users(Collections.singletonList(
                        new JobProfileUser()
                            .user(
                                new User()
                                    .internalId("internal-id")
                                    .externalId("john.doe")
                                    .fullName("John Doe")
                                    .identityLinkStrategy(IdentityUserLinkStrategy.CREATE_IN_IDENTITY)
                                    .locked(false)
                                    .emailAddress(new EmailAddress().address("test@example.com"))
                                    .mobileNumber(new PhoneNumber().number("+12345"))
                                    .limit(limitWithBussinessFunctionAndPrivileges())
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

    private static Limit limitWithBussinessFunctionAndPrivileges(){
        return new Limit()
            .addBusinessFunctionLimitsItem(new BusinessFunctionLimit("1001", false)
                    .addPrivilegesItem(new Privilege("create").limit(new Limit().transactional(BigDecimal.valueOf(2500d)).daily(BigDecimal.valueOf(2500d))))
                    .addPrivilegesItem(new Privilege("approve").limit(new Limit().transactional(BigDecimal.valueOf(2500d)).daily(BigDecimal.valueOf(2500d))))
            );
    }

    private void setupWireMock() {
        stubFor(
            WireMock.post("/oauth/token")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"access_token\": \"access-token\",\n\"expires_in\": 600,\n\"refresh_expires_in\": 1800,\n\"refresh_token\": \"refresh-token\",\n\"token_type\": \"bearer\",\n\"id_token\": \"id-token\",\n\"not-before-policy\": 1633622545,\n\"session_state\": \"72a28739-3d20-4965-bd86-64410df53d04\",\n\"scope\": \"openid\"\n}"))
        );

        stubFor(
            WireMock.get("/access-control/service-api/v3/accesscontrol/legal-entities/external/100000")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"type\":\"CUSTOMER\"\n}"))
        );

        stubFor(
            WireMock.get("/access-control/service-api/v3/accesscontrol/legal-entities/500000")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"type\":\"CUSTOMER\"\n}"))
        );

        stubFor(
            WireMock.put("/access-control/service-api/v3/accesscontrol/legal-entities")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\n\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"type\":\"CUSTOMER\"\n}"))
        );

        stubFor(
            WireMock.get("/user-manager/service-api/v2/users/identities/realms")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("[{\"id\":\"0006f11c\",\"realmName\":\"customer-bank\"}]"))
        );

        stubFor(
            WireMock.post("/user-manager/service-api/v2/users/identities/realms/customer-bank/legalentities")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        stubFor(
            WireMock.get("/user-manager/service-api/v2/users/externalids/john.doe?skipHierarchyCheck=true")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"id\":\"9ac44fca\",\"externalId\":\"john.doe\",\"legalEntityId\":\"500000\",\"fullName\":\"John Doe\"}"))
        );

        stubFor(
            WireMock.put("/user-manager/service-api/v2/users")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"id\":\"9ac44fca\",\"externalId\":\"john.doe\",\"legalEntityId\":\"500000\",\"fullName\":\"John Doe\"}"))
        );

        stubFor(
            WireMock.get("/access-control/service-api/v3/accesscontrol/service-agreements/external/Service_Agreement_Id")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"additions\":{},\"creatorLegalEntity\":\"500000\",\"status\":\"ENABLED\",\"id\":\"500001\",\"externalId\":\"Service_Agreement_Id\",\"name\":\"\",\"description\":\"Custom Service Agreement\",\"isMaster\":false,\"validFromDate\":null,\"validFromTime\":null,\"validUntilDate\":null,\"validUntilTime\":null}"))
        );

        stubFor(
            WireMock.get("/access-control/service-api/v3/accesscontrol/service-agreements/500001/participants")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"additions\":{},\"id\":\"500000\",\"externalId\":\"100000\",\"name\":\"Legal Entity\",\"sharingUsers\":true,\"sharingAccounts\":true}]"))
        );

        stubFor(
                WireMock.put("/access-control/service-api/v3/accesscontrol/service-agreements/ingest/participants")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        stubFor(
            WireMock.get("/access-control/service-api/v3/accesscontrol/service-agreements/500001/users")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.NOT_FOUND.value()))
        );

        stubFor(
                WireMock.put("/access-control/service-api/v3/accesscontrol/service-agreements/ingest/users")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        stubFor(
                WireMock.get("/access-control/service-api/v3/accesscontrol/function-groups?serviceAgreementId=500001")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"additions\":{},\"id\":\"500002\",\"serviceAgreementId\":\"500001\",\"name\":\"Private - Read only\",\"description\":\"Private - Read only\",\"type\":\"TEMPLATE\",\"permissions\":[{\"functionId\":\"1029\",\"assignedPrivileges\":[{\"additions\":{},\"privilege\":\"view\"}]}]},{\"additions\":{},\"id\":\"500003\",\"serviceAgreementId\":\"500001\",\"name\":\"Private - Full access\",\"description\":\"Private - Full access\",\"type\":\"TEMPLATE\",\"permissions\":[{\"functionId\":\"1029\",\"assignedPrivileges\":[{\"additions\":{},\"privilege\":\"create\"},{\"additions\":{},\"privilege\":\"edit\"},{\"additions\":{},\"privilege\":\"delete\"},{\"additions\":{},\"privilege\":\"execute\"},{\"additions\":{},\"privilege\":\"view\"}]}]}]"))
        );

        stubFor(
            WireMock.put("/access-control/service-api/v3/accesscontrol/function-groups/batch/update")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        stubFor(
            WireMock.put("/access-control/service-api/v3/accessgroups/users/permissions/user-permissions")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        stubFor(
                WireMock.get("/access-control/service-api/v3/accessgroups/users/9ac44fca/service-agreements/500001/permissions")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"additions\":{},\"approvalId\":null,\"items\":[{\"additions\":{},\"functionGroupId\":\"500002\",\"dataGroupIds\":[],\"selfApprovalPolicies\":[]},{\"additions\":{},\"functionGroupId\":\"500003\",\"dataGroupIds\":[],\"selfApprovalPolicies\":[]}]}"))
        );

        stubFor(
            WireMock.post("/arrangement-manager/service-api/v2/arrangements/batch")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );

        stubFor(
                WireMock.post("/access-control/service-api/v3/accesscontrol/data-groups")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.CREATED.value()))
        );

        stubFor(
                WireMock.get("/access-control/service-api/v3/accesscontrol/data-groups?serviceAgreementId=500001&includeItems=true")
                .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[{\"additions\":{},\"id\":\"4002\",\"name\":\"Default PG\",\"description\":\"Default data group description\",\"serviceAgreementId\":\"500001\",\"type\":\"ARRANGEMENTS\",\"approvalId\":null,\"items\":[\"arrangement-id-1\"]},{\"additions\":{},\"id\":\"4003\",\"name\":\"Mixed PG\",\"description\":\"Default data group description\",\"serviceAgreementId\":\"500001\",\"type\":\"ARRANGEMENTS\",\"approvalId\":null,\"items\":[\"arrangement-id-2\"]}]"))
        );

        stubFor(
            WireMock.put("/access-control/service-api/v3/accesscontrol/data-groups/batch/update/data-items")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.ACCEPTED.value()))
        );
        stubFor(
                WireMock.put("/access-control/service-api/v3/accesscontrol/service-agreements/500001")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.OK.value()))
        );
        stubFor(
            WireMock.post("/loan/service-api/v1/loans/batch")
                .willReturn(WireMock.aResponse().withStatus(HttpStatus.MULTI_STATUS.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("[{\"arrangementId\":\"arrId1\",\"resourceId\":\"resId1\"}]"))
        );
        stubFor(
                WireMock.get("/user-manager/service-api/v2/users/identities/9ac44fca")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.MULTI_STATUS.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("{\"internalId\":\"9ac44fca\",\"externalId\":\"externalId\"}"))
        );
        stubFor(
                WireMock.put("/user-manager/service-api/v2/users/identities/9ac44fca")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.MULTI_STATUS.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(""))
        );
        stubFor(
                WireMock.post("/limit/service-api/v2/limits/retrieval")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.OK.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(""))
        );
        stubFor(
                WireMock.post("/limit/service-api/v2/limits")
                        .willReturn(WireMock.aResponse().withStatus(HttpStatus.CREATED.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(""))
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
        webTestClient.post()
            .uri("/legal-entity")
            .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
            .bodyValue(legalEntityTask.getLegalEntity())
            .exchange()
            .expectStatus().isEqualTo(200);

        // Then
        verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/access-control/service-api/v3/accesscontrol/legal-entities/500000"))
            .withHeader("X-TID", WireMock.equalTo("tenant-id"))
            .withHeader("X-B3-TraceId", hexString())
            .withHeader("X-B3-SpanId", hexString()));
    }

    @Test
    void legalEntitySagaEmptyProductGroup() {
        // Given
        setupWireMock();
        LegalEntityTask legalEntityTask = defaultLegalEntityTask();
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
                .description("somePgDescription").savingAccounts(Collections.emptyList());
        legalEntityTask.getLegalEntity().productGroups(Collections.singletonList(productGroup));

        // When
        webTestClient.post()
                .uri("/legal-entity")
                .header("Content-Type", "application/json")
                .bodyValue(legalEntityTask.getLegalEntity())
                .exchange()
                .expectStatus().isEqualTo(200);

        // Then
        verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/access-control/service-api/v3/accesscontrol/legal-entities/500000"))
            .withHeader("X-B3-TraceId", hexString())
            .withHeader("X-B3-SpanId", hexString()));
    }

    @Test
    void legalEntitySagaUpdateLegalEntity() {
        // Given
        setupWireMock();
        LegalEntityTask legalEntityTask = defaultLegalEntityTask();

        // When
        webTestClient.post()
            .uri("/legal-entity")
            .header("Content-Type", "application/json")
            .header("X-TID", "tenant-id")
            .bodyValue(legalEntityTask.getLegalEntity())
            .exchange()
            .expectStatus().isEqualTo(200);

        webTestClient.post()
            .uri("/legal-entity")
            .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
            .bodyValue(legalEntityTask.getLegalEntity().name("Updated name"))
            .exchange()
            .expectStatus().isEqualTo(200);


        // Then
        verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/access-control/service-api/v3/accesscontrol/legal-entities/500000"))
            .withHeader("X-TID", WireMock.equalTo("tenant-id"))
            .withHeader("X-B3-TraceId", hexString())
            .withHeader("X-B3-SpanId", hexString()));
    }

    @Test
    void legalEntitySagaSetLimitLegalEntity() {
        // Given
        setupWireMock();
        LegalEntityTask legalEntityTask = defaultLegalEntityTask();

        // When
        webTestClient.post()
                .uri("/legal-entity")
                .header("Content-Type", "application/json")
                .header("X-TID", "tenant-id")
                .bodyValue(legalEntityTask.getLegalEntity())
                .exchange()
                .expectStatus().isEqualTo(200);


        // Then
        verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/limit/service-api/v2/limits/retrieval"))
                .withHeader("X-TID", WireMock.equalTo("tenant-id"))
                .withHeader("X-B3-TraceId", hexString())
                .withHeader("X-B3-SpanId", hexString()));

        verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/limit/service-api/v2/limits"))
                .withHeader("X-TID", WireMock.equalTo("tenant-id"))
                .withHeader("X-B3-TraceId", hexString())
                .withHeader("X-B3-SpanId", hexString()));
    }

    public static RegexPattern hexString() {
        return new RegexPattern("^[0-9a-f]+$");
    }
}
