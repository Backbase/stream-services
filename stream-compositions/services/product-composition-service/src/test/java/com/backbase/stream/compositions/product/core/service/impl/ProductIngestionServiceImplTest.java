package com.backbase.stream.compositions.product.core.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.events.egress.event.spec.v1.ProductCompletedEvent;
import com.backbase.stream.compositions.paymentorder.client.PaymentOrderCompositionApi;
import com.backbase.stream.compositions.paymentorder.client.model.PaymentOrderIngestionResponse;
import com.backbase.stream.compositions.paymentorder.client.model.PaymentOrderPostResponse;
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
import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.BookedBalance;
import com.backbase.stream.legalentity.model.CreditCard;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.TermDeposit;
import com.backbase.stream.legalentity.model.TermUnit;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductIngestionServiceImplTest {

  @Mock Validator validator;
  ProductPostIngestionService productPostIngestionService;
  @Mock BatchProductIngestionSaga batchProductIngestionSaga;
  @Mock EventBus eventBus;
  ProductConfigurationProperties config = new ProductConfigurationProperties();
  @Mock TransactionCompositionApi transactionCompositionApi;
  @Mock PaymentOrderCompositionApi paymentOrderCompositionApi;
  ProductGroupMapper mapper = Mappers.getMapper(ProductGroupMapper.class);
  private ProductIngestionService productIngestionService;
  @Mock private ProductIntegrationService productIntegrationService;

  @BeforeEach
  void setUp() {

    productPostIngestionService =
        new ProductPostIngestionServiceImpl(
            eventBus, config, transactionCompositionApi, paymentOrderCompositionApi, mapper);

    productIngestionService =
        new ProductIngestionServiceImpl(
            batchProductIngestionSaga,
            productIntegrationService,
            config,
            validator,
            productPostIngestionService);
  }

  @Test
  void ingestionInPullMode_Failure() {
    ProductIngestPullRequest productIngestPullRequest =
        ProductIngestPullRequest.builder().legalEntityExternalId("externalId").build();

    when(productIntegrationService.pullProductGroup(productIngestPullRequest))
        .thenReturn(Mono.error(new RuntimeException("error")));

    Events events = new Events();
    events.setEnableFailed(Boolean.TRUE);
    config.setEvents(events);
    Mono<ProductIngestResponse> productIngestResponse =
        productIngestionService.ingestPull(productIngestPullRequest);
    StepVerifier.create(productIngestResponse).expectError().verify();
  }

  @Test
  @Tag("true")
  void ingestionInPullModeAsync_success(TestInfo testInfo) {
    executeIngestionWithPullMode(getTagInfo(testInfo), null);
  }

  @Test
  @Tag("false")
  void ingestionInPullModeSync_success(TestInfo testInfo) {
    executeIngestionWithPullMode(getTagInfo(testInfo), null);
  }

  @Test
  void ingestionInPullSkipChainPerRequest_success() {
    executeIngestionWithPullMode(true, Boolean.FALSE);
  }

  Boolean getTagInfo(TestInfo testInfo) {
    String testConfig = testInfo.getTags().stream().findFirst().orElse("false");
    return Boolean.valueOf(testConfig);
  }

  void executeIngestionWithPullMode(Boolean isChainAsync, Boolean chainEnabledPerRequest) {
    configureEventAndChainParameters(isChainAsync, Boolean.TRUE);

    ProductIngestPullRequest productIngestPullRequest =
        ProductIngestPullRequest.builder()
            .source("nightly_ingestion")
            .serviceAgreementExternalId("sa_externalId")
            .serviceAgreementInternalId("sa_internalId")
            .legalEntityExternalId("le_externalId")
            .legalEntityInternalId("le_internalId")
            .userExternalId("user_externalId")
            .userInternalId("user_internalId")
            .source("source_of_ingestion_process")
            .additions(Map.of("addition", "addition1"))
            .paymentOrderChainEnabled(chainEnabledPerRequest)
            .transactionChainEnabled(chainEnabledPerRequest)
            .build();

    SavingsAccount savingsAccount = buildSavingsAccount();
    CurrentAccount currentAccount = buildCurrentAccount();
    Loan loan = buildLoanAccount();
    TermDeposit termDeposit = buildTermDeposit();
    CreditCard creditCard = buildCreditCard();

    ProductGroup productGroup = new ProductGroup();
    productGroup.setServiceAgreement(new ServiceAgreement().internalId("sa_internalId"));

    productGroup
        .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
        .name("somePgName")
        .description("somePgDescription")
        .savingAccounts(Collections.singletonList(savingsAccount))
        .currentAccounts(Collections.singletonList(currentAccount))
        .loans(Collections.singletonList(loan))
        .termDeposits(Collections.singletonList(termDeposit))
        .creditCards(Collections.singletonList(creditCard));

    ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
    Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

    ProductIngestResponse ingestResponse =
        new ProductIngestResponse(
            "sa_externalId", "sa_internalId", Collections.singletonList(productGroup), Map.of());
    ingestResponse.setLegalEntityExternalId("le_externalId");
    ingestResponse.setLegalEntityInternalId("le_internalId");
    ingestResponse.setUserExternalId("user_externalId");
    ingestResponse.setUserInternalId("user_internalId");
    ingestResponse.setSource("source_of_ingestion_process");
    ingestResponse.setAdditions(Map.of("addition", "addition1"));
    when(productIntegrationService.pullProductGroup(productIngestPullRequest))
        .thenReturn(Mono.just(ingestResponse));

    lenient()
        .when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
        .thenReturn(productGroupTaskMono);

    when(batchProductIngestionSaga.process(any(BatchProductGroupTask.class)))
        .thenReturn(
            Mono.just(
                new BatchProductGroupTask()
                    .data(
                        new BatchProductGroup()
                            .productGroups(List.of(productGroup))
                            .serviceAgreement(productGroup.getServiceAgreement()))));
    when(transactionCompositionApi.pullTransactions(any()))
        .thenReturn(
            Mono.just(
                new TransactionIngestionResponse()
                    .withTransactions(
                        List.of(
                            new TransactionsPostResponseBody()
                                .withId("id")
                                .withExternalId("externalId")))));
    doReturn(
            Mono.just(
                new PaymentOrderIngestionResponse()
                    .withNewPaymentOrder(List.of(new PaymentOrderPostResponse().withId("id")))))
        .when(paymentOrderCompositionApi)
        .pullPaymentOrder(any());
    Mono<ProductIngestResponse> productIngestResponse =
        productIngestionService.ingestPull(productIngestPullRequest);
    StepVerifier.create(productIngestResponse)
        .assertNext(Assertions::assertNotNull)
        .verifyComplete();

    verifyProductCompletedEvent();
  }

  private void verifyProductCompletedEvent() {
    ArgumentCaptor<EnvelopedEvent<ProductCompletedEvent>> argumentCaptor =
        ArgumentCaptor.forClass(EnvelopedEvent.class);
    verify(eventBus, times(1)).emitEvent(argumentCaptor.capture());
    ProductCompletedEvent event = argumentCaptor.getValue().getEvent();

    Map<String, String> additions = event.getAdditions();
    assertThat(additions.get("addition"), is("addition1"));
    assertThat(event.getSource(), is("source_of_ingestion_process"));
    assertThat(event.getLegalEntityExternalId(), is("le_externalId"));
    assertThat(event.getLegalEntityInternalId(), is("le_internalId"));
    assertThat(event.getUserExternalId(), is("user_externalId"));
    assertThat(event.getUserInternalId(), is("user_internalId"));
    assertThat(event.getProductGroups().get(0).getCurrentAccounts(), notNullValue());
    assertThat(event.getProductGroups().get(0).getSavingAccounts(), notNullValue());
    assertThat(event.getProductGroups().get(0).getLoans(), notNullValue());
    assertThat(event.getProductGroups().get(0).getTermDeposits(), notNullValue());
    assertThat(event.getProductGroups().get(0).getCreditCards(), notNullValue());
  }

  @NotNull
  private TermDeposit buildTermDeposit() {
    TermDeposit termDeposit =
        new TermDeposit()
            .termNumber(BigDecimal.valueOf(21212))
            .termUnit(TermUnit.DAILY)
            .BBAN("777151235")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
    termDeposit
        .externalId("termExtId")
        .productTypeExternalId("Term Deposit")
        .currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId("termInternalId")));
    return termDeposit;
  }

  @NotNull
  private CreditCard buildCreditCard() {
    CreditCard creditCard =
        new CreditCard()
            .availableBalance(new AvailableBalance().amount(BigDecimal.valueOf(100)))
            .BBAN("777151236")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
    creditCard
        .externalId("ccExtId")
        .productTypeExternalId("Credit Card")
        .currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId("ccInternalId")));
    return creditCard;
  }

  @NotNull
  private SavingsAccount buildSavingsAccount() {
    SavingsAccount savingsAccount = new SavingsAccount();
    savingsAccount
        .externalId("someAccountExId")
        .productTypeExternalId("Account")
        .currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId("savInternalId")));
    return savingsAccount;
  }

  @NotNull
  private CurrentAccount buildCurrentAccount() {
    CurrentAccount currentAccount =
        new CurrentAccount()
            .availableBalance(new AvailableBalance().amount(BigDecimal.valueOf(100)))
            .BBAN("777151234")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
    currentAccount
        .externalId("currentAccountExtId")
        .productTypeExternalId("Current Account")
        .currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId("currInternalId")));
    return currentAccount;
  }

  @NotNull
  private Loan buildLoanAccount() {
    Loan loan =
        new Loan()
            .availableBalance(new AvailableBalance().amount(BigDecimal.valueOf(100)))
            .BBAN("777151238")
            .accountHolderName("John Doe")
            .bookedBalance(new BookedBalance().amount(BigDecimal.valueOf(50)));
    loan.externalId("loanAccountExtId")
        .productTypeExternalId("Loan")
        .currency("GBP")
        .legalEntities(List.of(new LegalEntityReference().externalId("loanInternalId")));
    return loan;
  }

  private void configureEventAndChainParameters(Boolean isAsync, Boolean isCompositionEnabled) {
    Events events = new Events();
    events.setEnableCompleted(Boolean.TRUE);
    config.setEvents(events);

    Chains chains = new Chains();
    TransactionComposition transactionComposition = new TransactionComposition();
    transactionComposition.setAsync(isAsync);
    transactionComposition.setEnabled(isCompositionEnabled);
    chains.setTransactionComposition(transactionComposition);
    ProductConfigurationProperties.PaymentOrderComposition paymentOrderComposition =
        new ProductConfigurationProperties.PaymentOrderComposition();
    paymentOrderComposition.setAsync(isAsync);
    paymentOrderComposition.setEnabled(isCompositionEnabled);
    chains.setPaymentOrderComposition(paymentOrderComposition);
    config.setChains(chains);
  }

  @Test
  void ingestionInPushMode_Success() {
    executeIngestionInPushMode(false);
  }

  void executeIngestionInPushMode(Boolean isAsync) {
    configureEventAndChainParameters(isAsync, Boolean.FALSE);

    SavingsAccount account = buildSavingsAccount();
    ProductGroup productGroup = new ProductGroup();
    productGroup.setServiceAgreement(new ServiceAgreement().internalId("sa_internalId"));
    productGroup
        .productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS)
        .name("somePgName")
        .description("somePgDescription")
        .savingAccounts(Collections.singletonList(account));

    ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
    Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

    ProductIngestPushRequest productIngestPushRequest =
        ProductIngestPushRequest.builder()
            .productGroup(productGroup)
            .source("source_of_ingestion_process")
            .build();

    lenient()
        .when(batchProductIngestionSaga.process(any(ProductGroupTask.class)))
        .thenReturn(productGroupTaskMono);

    when(batchProductIngestionSaga.process(any(BatchProductGroupTask.class)))
        .thenReturn(
            Mono.just(
                new BatchProductGroupTask()
                    .data(
                        new BatchProductGroup()
                            .productGroups(List.of(productGroup))
                            .serviceAgreement(productGroup.getServiceAgreement()))));

    Mono<ProductIngestResponse> productIngestResponse =
        productIngestionService.ingestPush(productIngestPushRequest);
    StepVerifier.create(productIngestResponse)
        .assertNext(Assertions::assertNotNull)
        .verifyComplete();

    ArgumentCaptor<EnvelopedEvent<ProductCompletedEvent>> argumentCaptor =
        ArgumentCaptor.forClass(EnvelopedEvent.class);
    verify(eventBus, times(1)).emitEvent(argumentCaptor.capture());
    ProductCompletedEvent event = argumentCaptor.getValue().getEvent();
    assertThat(event.getSource(), is("source_of_ingestion_process"));
  }
}
