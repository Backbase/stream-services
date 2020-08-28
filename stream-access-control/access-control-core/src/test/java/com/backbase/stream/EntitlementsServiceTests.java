package com.backbase.stream;

import com.backbase.dbs.accesscontrol.query.service.api.AccesscontrolApi;
import com.backbase.dbs.accessgroup.presentation.service.ApiClient;
import com.backbase.dbs.accessgroup.presentation.service.api.AccessgroupsApi;
import com.backbase.dbs.accounts.presentation.service.api.ArrangementsApi;
import com.backbase.dbs.legalentity.presentation.service.api.LegalentitiesApi;
import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.stream.legalentity.model.AssignedPermission;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.service.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Ignore
public class EntitlementsServiceTests extends AbstractServiceIntegrationTests {

    private static final String PRIVATE_INGRESS = "https://stream-api.proto.backbasecloud.com";
    private EntitlementsService entitlementsService;

    @Before
    public void setup() {
        String tokenUri = "https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token";
        WebClient webClient = super.setupWebClientBuilder(tokenUri, "bb-client", "bb-secret");

        ArrangementsApi arrangementsApi = new ArrangementsApi(getAccountsPresentationClient(webClient));
        UsersApi usersApi = new UsersApi(getUsersApiClient(webClient));
        AccessgroupsApi accessgroupsApi = new AccessgroupsApi(getAccessGroupApiClient(webClient));
        AccesscontrolApi accesscontrolApi = new AccesscontrolApi(getAccessControlQueryApi(webClient));


        LegalentitiesApi legalEntityApi = new LegalentitiesApi(getLegalEntityApiClient(webClient));

        ArrangementService arrangementService = new ArrangementService(arrangementsApi);
        UserService userService = new UserService(usersApi);
        AccessGroupService accessGroupService = new AccessGroupService(accesscontrolApi, accessgroupsApi, usersApi);
        LegalEntityService legalEntityService = new LegalEntityService(legalEntityApi, accesscontrolApi);

        entitlementsService = new EntitlementsService(
            arrangementService,
            userService,
            accessGroupService,
            legalEntityService);
    }

    @Test
    public void testGetAssignedPermissions() {
        List<AssignedPermission> assignedPermissions = entitlementsService.getAssignedPermissions(
            "john",
            "Transactions",
            "Transactions",
            "view")
            .collectList()
            .block();

        System.out.println(assignedPermissions);
    }

    private com.backbase.dbs.user.presentation.service.ApiClient getUsersApiClient(WebClient webClient) {

        com.backbase.dbs.user.presentation.service.ApiClient apiClient =
            new com.backbase.dbs.user.presentation.service.ApiClient(webClient, getObjectMapper(), getDateFormat());
        apiClient.setBasePath(PRIVATE_INGRESS + "/user-presentation-service/service-api/v2");
        return apiClient;
    }

    private ApiClient getAccessGroupApiClient(WebClient webClient) {
        ApiClient apiClient = new ApiClient(webClient, getObjectMapper(), getDateFormat());
        apiClient.setBasePath(PRIVATE_INGRESS + "/accessgroup-presentation-service/service-api/v2");
        return apiClient;
    }

    private com.backbase.dbs.accesscontrol.query.service.ApiClient getAccessControlQueryApi(WebClient webClient) {
        com.backbase.dbs.accesscontrol.query.service.ApiClient apiClient = new
            com.backbase.dbs.accesscontrol.query.service.ApiClient(webClient, getObjectMapper(), getDateFormat());
        apiClient.setBasePath(PRIVATE_INGRESS + "/accessgroup-presentation-service/service-api/v2");
        return apiClient;
    }

    private com.backbase.dbs.accounts.presentation.service.ApiClient getAccountsPresentationClient(
        WebClient webClient) {
        return new com.backbase.dbs.accounts.presentation.service.ApiClient(webClient,
            getObjectMapper(), getDateFormat())
            .setBasePath(PRIVATE_INGRESS + "/account-presentation-service/service-api/v2");
    }

    private com.backbase.dbs.legalentity.presentation.service.ApiClient getLegalEntityApiClient(WebClient webClient) {
        com.backbase.dbs.legalentity.presentation.service.ApiClient apiClient = new
            com.backbase.dbs.legalentity.presentation.service.ApiClient(
            webClient, getObjectMapper(), getDateFormat());
        apiClient.setBasePath(PRIVATE_INGRESS + "/legalentity-presentation-service/service-api/v2");
        return apiClient;
    }


}
