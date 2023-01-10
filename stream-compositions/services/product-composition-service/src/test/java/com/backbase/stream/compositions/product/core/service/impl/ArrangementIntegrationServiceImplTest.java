package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.integration.product.api.ArrangementIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.AccountArrangementItemPut;
import com.backbase.stream.compositions.integration.product.model.PullArrangementResponse;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.mapper.ArrangementMapper;

import com.backbase.stream.compositions.product.core.model.*;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import com.backbase.stream.compositions.product.core.service.ArrangementIntegrationService;
import com.backbase.stream.compositions.product.core.service.ArrangementPostIngestionService;

import com.backbase.stream.compositions.product.util.JsonUtil;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.client.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.client.model.TransactionsPostResponseBody;
import com.backbase.stream.legalentity.model.*;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArrangementIntegrationServiceImplTest {

    private ArrangementIngestionService arrangementIngestionService;
    @Mock
    ArrangementIntegrationService arrangementIntegrationService;

    @Mock
    ArrangementIntegrationApi arrangementIntegrationApi;

    ProductConfigurationProperties config = new ProductConfigurationProperties();

    ArrangementPostIngestionService arrangementPostIngestionService;

    @Mock
    ArrangementMapper arrangementMapper;

    @Mock
    TransactionCompositionApi transactionCompositionApi;
    
    @Mock
    ArrangementService arrangementService;

    @Mock
    EventBus eventBus;

    @Mock
    Validator validator;

    ArrangementMapper mapper = Mappers.getMapper(ArrangementMapper.class);

    @BeforeEach
    void setUp() {
        arrangementPostIngestionService = new ArrangementPostIngestionServiceImpl(eventBus, config,transactionCompositionApi);
        arrangementIngestionService = new ArrangementIngestionServiceImpl(arrangementService,arrangementIntegrationService,arrangementPostIngestionService,validator);
    }


    @Test
    void ingestionInPullMode_Failure() {
        ArrangementIngestPullRequest request = ArrangementIngestPullRequest
                .builder()
                .arrangementId("arrangementId")
                .build();

        when(arrangementIntegrationService.pullArrangement(request))
                .thenReturn(Mono.error(new RuntimeException("error")));

        ProductConfigurationProperties.Events events = new ProductConfigurationProperties.Events();
        events.setEnableFailed(Boolean.TRUE);
        config.setEvents(events);

        Mono<ArrangementIngestResponse> responseMono  = arrangementIngestionService
                .ingestPull(request);
        StepVerifier.create(responseMono)
                .expectError().verify();

    }

    @Test
    @Tag("true")
    void ingestionInPullModeAsync_success(TestInfo testInfo) {
        executeIngestionWithPullMode(getTagInfo(testInfo));
    }

    @Test
    @Tag("false")
    void ingestionInPullModeSync_success(TestInfo testInfo) {
        executeIngestionWithPullMode(getTagInfo(testInfo));
    }

    Boolean getTagInfo(TestInfo testInfo) {
        String testConfig = testInfo.getTags().stream().findFirst().orElse("false");
        return Boolean.valueOf(testConfig);
    }

    void executeIngestionWithPullMode(Boolean isAsync) {

        ProductConfigurationProperties.Events events = new ProductConfigurationProperties.Events();
        events.setEnableCompleted(Boolean.TRUE);
        config.setEvents(events);

        ProductConfigurationProperties.Chains chains = new ProductConfigurationProperties.Chains();
        ProductConfigurationProperties.TransactionComposition transactionComposition = new ProductConfigurationProperties.TransactionComposition();
        transactionComposition.setAsync(isAsync);
        transactionComposition.setEnabled(Boolean.TRUE);
        chains.setTransactionComposition(transactionComposition);

        ArrangementIngestPullRequest productIngestPullRequest = ArrangementIngestPullRequest.builder().arrangementId("internalId").externalArrangementId("externalId")
                .build();

        com.backbase.stream.compositions.integration.product.model.AccountArrangementItemPut itemPut = new  com.backbase.stream.compositions.integration.product.model.AccountArrangementItemPut();
        itemPut.setExternalArrangementId("ID-1");
        itemPut.setCurrency("USD");
        itemPut.setProductId("123212");
        
        Mono<com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut> accountArrangementItemPutMono = Mono.just((mapper.mapIntegrationToStream(itemPut)));
        RequestConfig config = JsonUtil.readJsonFileToObject(RequestConfig.class,"integration-data/request-config.json");
        when(arrangementIntegrationService.pullArrangement(productIngestPullRequest))
                .thenReturn(Mono.just(new ArrangementIngestResponse(mapper.mapIntegrationToStream(itemPut), "id2", "testcase", config)));

        lenient().when(arrangementService.updateArrangement(any(com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut.class)))
                .thenReturn(accountArrangementItemPutMono);
        when(transactionCompositionApi.pullTransactions(any()))
                .thenReturn(Mono.just(new TransactionIngestionResponse()
                        .withTransactions(List.of(
                                new TransactionsPostResponseBody().withId("id").withExternalId("externalId")))));
        Mono<ArrangementIngestResponse> productIngestResponse = arrangementIngestionService
                .ingestPull(productIngestPullRequest);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();

    }


    @Test
    void ingestionInPushMode_Success() {
        executeIngestionInPushMode(false);
    }

    void executeIngestionInPushMode(Boolean isAsync) {
        ProductConfigurationProperties.Events events = new ProductConfigurationProperties.Events();
        events.setEnableCompleted(Boolean.TRUE);
        config.setEvents(events);

        ProductConfigurationProperties.Chains chains = new ProductConfigurationProperties.Chains();
        ProductConfigurationProperties.TransactionComposition transactionComposition = new ProductConfigurationProperties.TransactionComposition();
        transactionComposition.setAsync(isAsync);
        transactionComposition.setEnabled(Boolean.FALSE);
        chains.setTransactionComposition(transactionComposition);
        config.setChains(chains);

        RequestConfig config = JsonUtil.readJsonFileToObject(RequestConfig.class,"integration-data/request-config.json");
        com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut itemPut = new  com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut();
        itemPut.setExternalArrangementId("ID-1");
        itemPut.setCurrency("USD");
        itemPut.setProductId("123212");

        ArrangementIngestPushRequest productIngestPullRequest = ArrangementIngestPushRequest.builder().arrangementInternalId("internalId").arrangement(itemPut).config(config).build();

        Mono<com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut> accountArrangementItemPutMono = Mono.just((itemPut));


        lenient().when(arrangementService.updateArrangement(any(com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut.class)))
                .thenReturn(accountArrangementItemPutMono);

        when(transactionCompositionApi.pullTransactions(any()))
                .thenReturn(Mono.just(new TransactionIngestionResponse()
                        .withTransactions(List.of(
                                new TransactionsPostResponseBody().withId("id").withExternalId("externalId")))));

        Mono<ArrangementIngestResponse> productIngestResponse = arrangementIngestionService
                .ingestPush(productIngestPullRequest);
        StepVerifier.create(productIngestResponse)
                .assertNext(Assertions::assertNotNull).verifyComplete();
    }
}