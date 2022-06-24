package com.backbase.stream.compositions.product.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.Chains;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.Events;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties.TransactionComposition;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.compositions.product.core.service.ProductPostIngestionService;
import com.backbase.stream.compositions.transaction.client.TransactionCompositionApi;
import com.backbase.stream.compositions.transaction.client.model.TransactionIngestionResponse;
import com.backbase.stream.compositions.transaction.client.model.TransactionsPostResponseBody;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductIngestionServiceImplTest {

  private ProductIngestionService productIngestionService;

  @Mock
  private ProductIntegrationService productIntegrationService;

  @Mock
  Validator validator;

  ProductPostIngestionService productPostIngestionService;

  @Mock
  BatchProductIngestionSaga batchProductIngestionSaga;

  @Mock
  EventBus eventBus;

  ProductConfigurationProperties config = new ProductConfigurationProperties();

  @Mock
  TransactionCompositionApi transactionCompositionApi;

  ProductGroupMapper mapper = Mappers.getMapper(ProductGroupMapper.class);


  @BeforeEach
  void setUp() {

    productPostIngestionService = new ProductPostIngestionServiceImpl(eventBus, config,
        transactionCompositionApi, mapper);

    productIngestionService = new ProductIngestionServiceImpl(
        batchProductIngestionSaga,
        productIntegrationService,
        validator,
        productPostIngestionService);
  }

  @Test
  void ingestionInPullMode_Failure() {
    ProductIngestPullRequest productIngestPullRequest = ProductIngestPullRequest.builder()
        .legalEntityExternalId("externalId")
        .build();

    when(productIntegrationService.pullProductGroup(productIngestPullRequest))
        .thenReturn(Mono.error(new RuntimeException("error")));

    Events events = new Events();
    events.setEnableFailed(Boolean.TRUE);
    config.setEvents(events);
    Mono<ProductIngestResponse> productIngestResponse = productIngestionService
        .ingestPull(productIngestPullRequest);
    StepVerifier.create(productIngestResponse)
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

    Events events = new Events();
    events.setEnableCompleted(Boolean.TRUE);
    config.setEvents(events);

    Chains chains = new Chains();
    TransactionComposition transactionComposition = new TransactionComposition();
    transactionComposition.setAsync(isAsync);
    transactionComposition.setEnabled(Boolean.TRUE);
    chains.setTransactionComposition(transactionComposition);
    config.setChains(chains);

    ProductIngestPullRequest productIngestPullRequest = ProductIngestPullRequest.builder()
        .legalEntityExternalId("externalId")
        .build();

    SavingsAccount account = new SavingsAccount();
    account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId("savInternalId")));
    ProductGroup productGroup = new ProductGroup();
    productGroup.setServiceAgreement(new ServiceAgreement().internalId("sa_internalId"));
    productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
        .name("somePgName")
        .description("somePgDescription").savingAccounts(Collections.singletonList(account));

    ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
    Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

    when(productIntegrationService.pullProductGroup(productIngestPullRequest))
        .thenReturn(Mono.just(new ProductIngestResponse(productGroup, Map.of())));

    lenient().when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
        .thenReturn(productGroupTaskMono);

    when(batchProductIngestionSaga.process(any(BatchProductGroupTask.class)))
        .thenReturn(Mono.just(new BatchProductGroupTask()
            .data(new BatchProductGroup().productGroups(List.of(productGroup))
                .serviceAgreement(productGroup.getServiceAgreement()))));
    when(transactionCompositionApi.pullTransactions(any()))
        .thenReturn(Mono.just(new TransactionIngestionResponse()
            .withTransactions(List.of(
                new TransactionsPostResponseBody().withId("id").withExternalId("externalId")))));
    Mono<ProductIngestResponse> productIngestResponse = productIngestionService
        .ingestPull(productIngestPullRequest);
    StepVerifier.create(productIngestResponse)
        .assertNext(Assertions::assertNotNull).verifyComplete();

  }

  @Test
  void ingestionInPushMode_Unsupported() {
    ProductIngestPushRequest request = ProductIngestPushRequest.builder().build();
    assertThrows(UnsupportedOperationException.class, () -> {
      productIngestionService.ingestPush(request);
    });
  }
}