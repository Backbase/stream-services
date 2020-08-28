package com.backbase.stream.legalentity;

import brave.Tracer;
import brave.Tracing;
import com.backbase.dbs.accesscontrol.query.service.api.AccesscontrolApi;
import com.backbase.dbs.accessgroup.presentation.service.ApiClient;
import com.backbase.dbs.accessgroup.presentation.service.api.AccessgroupsApi;
import com.backbase.dbs.accounts.presentation.service.api.ArrangementsApi;
import com.backbase.dbs.legalentity.presentation.service.api.LegalentitiesApi;
import com.backbase.dbs.user.presentation.service.api.UsersApi;
import com.backbase.stream.AbstractServiceIntegrationTests;
import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityUnitOfWorkExecutor;
import com.backbase.stream.configuration.LegalEntitySagaConfiguration;
import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.legalentity.generator.LegalEntityGenerator;
import com.backbase.stream.legalentity.generator.configuration.LegalEntityGeneratorConfigurationProperties;
import com.backbase.stream.legalentity.repository.LegalEntityUnitOfWorkRepository;
import com.backbase.stream.product.ProductIngestionSaga;
import com.backbase.stream.product.configuration.ProductIngestionSagaConfigurationProperties;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.EntitlementsService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.springframework.web.reactive.function.client.WebClient;

@SuppressWarnings("ALL")
@Slf4j
@Ignore
public class AbstractLegalEntityCoreTests extends AbstractServiceIntegrationTests {


    protected LegalEntitySaga legalEntitySaga;
    protected ProductIngestionSaga productIngestionSaga;
    protected AccessGroupService accessGroupService;
    protected ArrangementService arrangementService;
    protected EntitlementsService entitlementsService;
    protected LegalEntityUnitOfWorkExecutor unitOfWorkExecutor;
    protected LegalEntityUnitOfWorkRepository unitOfWorkRepository;

    protected LegalEntityGenerator legalEntityGenerator;
    protected ReactiveProductCatalogService productCatalogService;

    Tracer tracer = Tracing.newBuilder()
        .localServiceName("legal-entity-core")
        .build()
        .tracer();


    @Before
    public void setup() {

        String tokenUri = "https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token";
        WebClient webClient = super.setupWebClientBuilder(tokenUri, "bb-client", "bb-secret");

//        ReactiveRedisTemplate<String, String> redisTemplate = getReactiveRedisTemplate();
        LegalentitiesApi legalEntityApi = new LegalentitiesApi(getLegalEntityApiClient(webClient));
        AccesscontrolApi accesscontrolApi = new AccesscontrolApi(getAccessControlQueryApi(webClient));
        LegalEntityService legalEntityService = new LegalEntityService(legalEntityApi, accesscontrolApi);
        UsersApi usersApi = new UsersApi(getUsersApiClient(webClient));
        UserService userService = new UserService(usersApi);

        com.backbase.dbs.accounts.presentation.service.ApiClient accountsPresentationClient = getAccountsPresentationClient(
            webClient);

        arrangementService = new ArrangementService(new ArrangementsApi(accountsPresentationClient));

        AccessgroupsApi accessGroupServiceApi = new AccessgroupsApi(getAccessGroupApiClient(webClient));
        accessGroupService = new AccessGroupService(accesscontrolApi, accessGroupServiceApi, usersApi);

        entitlementsService = new EntitlementsService(arrangementService, userService, accessGroupService,
            legalEntityService);

        productIngestionSaga = new ProductIngestionSaga(arrangementService,
            accessGroupService,
            userService,
            new ProductIngestionSagaConfigurationProperties());

        productCatalogService = new ReactiveProductCatalogService(accountsPresentationClient);


        LegalEntitySagaConfigurationProperties sinkConfigurationProperties = new LegalEntitySagaConfigurationProperties();
        ObjectMapper objectMapper = getObjectMapper();
        this.legalEntitySaga = new LegalEntitySaga(legalEntityService,
            userService,
            accessGroupService,
            productIngestionSaga,
            sinkConfigurationProperties,
            objectMapper);

        unitOfWorkRepository = new LegalEntitySagaConfiguration.LegalEntityInMemoryUnitOfWorkRepository();

        unitOfWorkExecutor = new LegalEntityUnitOfWorkExecutor(unitOfWorkRepository, legalEntitySaga, new LegalEntitySagaConfigurationProperties());

        LegalEntityGeneratorConfigurationProperties legalEntityGeneratorConfigurationProperties = TestUtils.getLegalEntityGeneratorConfigurationProperties();

    }


    protected com.backbase.dbs.user.presentation.service.ApiClient getUsersApiClient(WebClient webClient) {
        com.backbase.dbs.user.presentation.service.ApiClient apiClient = new com.backbase.dbs.user.presentation.service.ApiClient(
            webClient, getObjectMapper(), getDateFormat());
        apiClient.setBasePath("https://stream-api.proto.backbasecloud.com/user-presentation-service/service-api/v2");
        return apiClient;
    }

    protected ApiClient getAccessGroupApiClient(WebClient webClient) {
        ApiClient apiClient = new ApiClient(webClient,
            getObjectMapper(), getDateFormat());
        apiClient
            .setBasePath("https://stream-api.proto.backbasecloud.com/accessgroup-presentation-service/service-api/v2");
        return apiClient;
    }

    protected com.backbase.dbs.accounts.presentation.service.ApiClient getAccountsPresentationClient(
        WebClient webClient) {
        return new com.backbase.dbs.accounts.presentation.service.ApiClient(webClient, getObjectMapper(),
            getDateFormat())
            .setBasePath("https://stream-api.proto.backbasecloud.com/account-presentation-service/service-api/v2");
    }

    protected com.backbase.dbs.legalentity.presentation.service.ApiClient getLegalEntityApiClient(WebClient webClient) {
        com.backbase.dbs.legalentity.presentation.service.ApiClient apiClient = new com.backbase.dbs.legalentity.presentation.service.ApiClient(
            webClient, getObjectMapper(), getDateFormat());
        apiClient
            .setBasePath("https://stream-api.proto.backbasecloud.com/legalentity-presentation-service/service-api/v2");
        return apiClient;
    }

    protected com.backbase.dbs.accesscontrol.query.service.ApiClient getAccessControlQueryApi(WebClient webClient) {
        com.backbase.dbs.accesscontrol.query.service.ApiClient apiClient = new com.backbase.dbs.accesscontrol.query.service.ApiClient(
            webClient, getObjectMapper(), getDateFormat());
        apiClient.setBasePath(
            "https://stream-api.proto.backbasecloud.com/accesscontrol-pandp-service/service-api/v2");
        return apiClient;
    }

}