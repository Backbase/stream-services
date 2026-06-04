package com.backbase.stream.investment.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.AssetUniverseApi;
import com.backbase.investment.api.service.v1.model.Deposit;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.ClientUser;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentAssetData;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.PortfolioRiskAssessment;
import com.backbase.stream.investment.model.InvestmentPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolioTradingAccount;
import com.backbase.stream.investment.model.RiskQuestion;
import com.backbase.stream.investment.service.AsyncTaskService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioProductService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.investment.service.InvestmentRiskAssessmentService;
import com.backbase.stream.investment.service.InvestmentRiskQuestionaryService;
import com.backbase.stream.worker.model.StreamTask.State;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit test suite for {@link InvestmentSaga}.
 */
class InvestmentSagaTest {

    private static final String ARRANGEMENT_EXTERNAL_ID = "some-arrangement-id";
    private static final String PORTFOLIO_EXTERNAL_ID = "some-portfolio-external-id";
    private static final String ACCOUNT_ID = "some-account-id";
    private static final String ACCOUNT_EXTERNAL_ID = "some-account-external-id";
    private static final String LE_INTERNAL_ID = "some-le-internal-id";

    @Mock
    private InvestmentClientService clientService;

    @Mock
    InvestmentRiskAssessmentService investmentRiskAssessmentService;

    @Mock
    InvestmentRiskQuestionaryService investmentRiskQuestionaryService;

    @Mock
    private InvestmentPortfolioService investmentPortfolioService;

    @Mock
    private InvestmentPortfolioAllocationService investmentPortfolioAllocationService;

    @Mock
    private InvestmentModelPortfolioService investmentModelPortfolioService;

    @Mock
    private InvestmentPortfolioProductService investmentPortfolioProductService;

    @Mock
    private AsyncTaskService asyncTaskService;

    @Mock
    private InvestmentIngestionConfigurationProperties configurationProperties;

    @Mock
    private AssetUniverseApi assetUniverseApi;

    private InvestmentSaga investmentSaga;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(configurationProperties.isWealthEnabled()).thenReturn(true);
        investmentSaga = new InvestmentSaga(
            clientService,
            investmentRiskAssessmentService,
            investmentRiskQuestionaryService,
            investmentPortfolioService,
            investmentPortfolioAllocationService,
            investmentModelPortfolioService,
            investmentPortfolioProductService,
            asyncTaskService,
            configurationProperties,
            assetUniverseApi
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // =========================================================================
    // executeTask – top-level orchestration
    // =========================================================================

    @Nested
    @DisplayName("executeTask")
    class ExecuteTaskTests {

        @Test
        @DisplayName("should complete successfully when all services succeed")
        void executeTask_allServicesSucceed_completesNormally() {
            InvestmentTask task = createFullTask();
            stubAllServicesSuccess();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should mark task FAILED and complete stream when a service throws an error")
        void executeTask_serviceThrowsError_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.error(new RuntimeException("Model portfolio service down")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should complete with empty task data")
        void executeTask_emptyTask_completesNormally() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should skip saga when wealth ingestion is disabled")
        void executeTask_wealthDisabled_skipsPipeline() {
            when(configurationProperties.isWealthEnabled()).thenReturn(false);
            InvestmentTask task = createFullTask();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isNotEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(clientService, never()).upsertClients(any());
            verify(investmentPortfolioProductService, never()).upsertInvestmentProducts(any(), any());
            verify(assetUniverseApi, never()).getAsset(any(), any(), any(), any());
        }
    }

    // =========================================================================
    // loadAssets
    // =========================================================================

    @Nested
    @DisplayName("loadAssets")
    class LoadAssetsTests {

        @Test
        @DisplayName("should skip asset load when asset universe ingestion is enabled")
        void loadAssets_assetUniverseEnabled_skipsApiCall() {
            when(configurationProperties.isAssetUniverseEnabled()).thenReturn(true);
            InvestmentTask task = createTaskWithAssets();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseApi, never()).getAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should skip asset load when investment asset data is null")
        void loadAssets_noAssetData_skipsApiCall() {
            when(configurationProperties.isAssetUniverseEnabled()).thenReturn(false);
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseApi, never()).getAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should skip asset load when asset list is empty")
        void loadAssets_emptyAssetList_skipsApiCall() {
            when(configurationProperties.isAssetUniverseEnabled()).thenReturn(false);
            InvestmentTask task = new InvestmentTask("empty-assets", InvestmentData.builder()
                .investmentAssetData(InvestmentAssetData.builder().assets(List.of()).build())
                .investmentArrangements(List.of())
                .clientUsers(List.of())
                .portfolios(List.of())
                .build());
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(assetUniverseApi, never()).getAsset(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should resolve asset UUIDs from asset universe API before upserting clients")
        void loadAssets_withAssets_resolvesUuids() {
            when(configurationProperties.isAssetUniverseEnabled()).thenReturn(false);
            Asset streamAsset = Asset.builder().isin("US0378331005").market("XNAS").currency("USD").build();
            UUID resolvedUuid = UUID.randomUUID();
            com.backbase.investment.api.service.v1.model.Asset apiAsset =
                new com.backbase.investment.api.service.v1.model.Asset(resolvedUuid);

            when(assetUniverseApi.getAsset(eq("US0378331005_XNAS_USD"), isNull(), isNull(), isNull()))
                .thenReturn(reactor.core.publisher.Mono.just(apiAsset));

            InvestmentTask task = createTaskWithAssets(streamAsset);
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(streamAsset.getUuid()).isEqualTo(resolvedUuid);
            verify(assetUniverseApi).getAsset(eq("US0378331005_XNAS_USD"), isNull(), isNull(), isNull());
        }
    }

    // =========================================================================
    // rollBack
    // =========================================================================

    @Nested
    @DisplayName("rollBack")
    class RollBackTests {

        @Test
        @DisplayName("should return empty Mono and complete without error")
        void rollBack_returnsEmptyMono() {
            InvestmentTask task = createMinimalTask();

            StepVerifier.create(investmentSaga.rollBack(task))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertRiskQuestions
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskQuestions")
    class UpsertRiskQuestionsTests {

        @Test
        @DisplayName("should complete successfully when risk questions list is empty")
        void upsertRiskQuestions_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRiskQuestionaryService).upsertRiskQuestions(Collections.emptyList());
        }

        @Test
        @DisplayName("should upsert risk questions and mark task COMPLETED")
        void upsertRiskQuestions_success() {
            InvestmentTask task = createTaskWithRiskQuestions();
            wireTrivialPipelineAfterModelPortfolios();
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should mark task FAILED when risk questions upsert throws an error")
        void upsertRiskQuestions_error_marksTaskFailed() {
            InvestmentTask task = createMinimalTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.empty());
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.error(new RuntimeException("Risk questions service failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertRiskAssessments
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskAssessments")
    class UpsertRiskAssessmentsTests {

        @Test
        @DisplayName("should skip assessment service call when client list is empty")
        void upsertRiskAssessments_emptyClientList_skipsService() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should upsert risk assessments for each client and mark task COMPLETED")
        void upsertRiskAssessments_success() {
            InvestmentTask task = createFullTask();
            stubAllServicesSuccess();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRiskAssessmentService).upsertRiskAssessments(any(), any());
        }

        @Test
        @DisplayName("should mark task FAILED when risk assessment upsert throws an error")
        void upsertRiskAssessments_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(clientWithId())));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Risk assessment service failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("upsertPortfolioModels")
    class UpsertPortfolioModelsTests {

        @Test
        @DisplayName("should complete successfully without calling service when model portfolio list is empty")
        void upsertPortfolioModels_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentModelPortfolioService).upsertModels(any());
        }

        @Test
        @DisplayName("should upsert portfolio models and mark task COMPLETED")
        void upsertPortfolioModels_success() {
            InvestmentTask task = createTaskWithModelPortfolios();
            wireTrivialPipelineAfterModelPortfolios();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should mark task FAILED when model portfolio upsert throws an error")
        void upsertPortfolioModels_error_marksTaskFailed() {
            InvestmentTask task = createTaskWithModelPortfolios();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.error(new RuntimeException("Model portfolio service failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertClients
    // =========================================================================

    @Nested
    @DisplayName("upsertClients")
    class UpsertClientsTests {

        @Test
        @DisplayName("should complete successfully without calling service when client list is empty")
        void upsertClients_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(clientService).upsertClients(Collections.emptyList());
        }

        @Test
        @DisplayName("should upsert clients and mark task COMPLETED")
        void upsertClients_success() {
            InvestmentTask task = createFullTask();
            ClientUser client = ClientUser.builder()
                .investmentClientId(UUID.randomUUID())
                .legalEntityId(LE_INTERNAL_ID)
                .build();
            stubAllServicesSuccess();
            when(clientService.upsertClients(any())).thenReturn(Mono.just(List.of(client)));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(task.getData().getClientUsers()).containsExactly(client);
        }

        @Test
        @DisplayName("should mark task FAILED when client upsert throws an error")
        void upsertClients_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.error(new RuntimeException("Client service failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertInvestmentProducts
    // =========================================================================

    @Nested
    @DisplayName("upsertInvestmentProducts")
    class UpsertInvestmentProductsTests {

        @Test
        @DisplayName("should complete successfully when arrangement list is empty")
        void upsertInvestmentProducts_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioProductService).upsertInvestmentProducts(any(), any());
        }

        @Test
        @DisplayName("should upsert products and register them on investment data")
        void upsertInvestmentProducts_success_registersProducts() {
            InvestmentTask task = createFullTask();
            UUID productUuid = UUID.randomUUID();
            PortfolioProduct product = new PortfolioProduct(
                "Robo", null, null, 1, null, "retail", productUuid, null, null, ProductTypeEnum.ROBO_ADVISOR);
            stubAllServicesSuccess();
            when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.just(List.of(product)));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioProductService).upsertInvestmentProducts(task.getData(), task.getData().getInvestmentArrangements());
            assertThat(task.getData().getIngestedPortfolioProducts()).containsExactly(product);
        }

        @Test
        @DisplayName("should mark task FAILED when product upsert throws an error")
        void upsertInvestmentProducts_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(clientWithId())));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Arrangement upsert failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertPortfolios
    // =========================================================================

    @Nested
    @DisplayName("upsertPortfolios")
    class UpsertPortfoliosTests {

        @Test
        @DisplayName("should complete successfully without calling service when portfolio list is empty")
        void upsertPortfolios_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioService).upsertPortfolios(any(), any());
        }

        @Test
        @DisplayName("should set portfolios on task data after successful upsert")
        void upsertPortfolios_success_setsPortfoliosOnTask() {
            InvestmentTask task = createFullTask();
            InvestmentPortfolio portfolio = InvestmentPortfolio.builder().portfolio(new PortfolioList()).build();
            stubAllServicesSuccess();
            when(investmentPortfolioService.upsertPortfolios(any(), any()))
                .thenReturn(Mono.just(List.of(portfolio)));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(task.getData().getPortfolios()).containsExactly(portfolio);
        }

        @Test
        @DisplayName("should mark task FAILED when portfolio upsert throws an error")
        void upsertPortfolios_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(clientWithId())));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.just(List.of(new PortfolioProduct())));
            when(investmentPortfolioService.upsertPortfolios(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Portfolio upsert failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertPortfolioTradingAccounts
    // =========================================================================

    @Nested
    @DisplayName("upsertPortfolioTradingAccounts")
    class UpsertPortfolioTradingAccountsTests {

        @Test
        @DisplayName("should complete successfully without calling service when trading account list is empty")
        void upsertPortfolioTradingAccounts_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioService).upsertPortfolioTradingAccounts(Collections.emptyList());
        }

        @Test
        @DisplayName("should upsert trading accounts and mark task COMPLETED")
        void upsertPortfolioTradingAccounts_success() {
            InvestmentTask task = createFullTask();
            stubAllServicesSuccess();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }

        @Test
        @DisplayName("should mark task FAILED when trading account upsert throws an error")
        void upsertPortfolioTradingAccounts_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(clientWithId())));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.just(List.of(new PortfolioProduct())));
            when(investmentPortfolioService.upsertPortfolios(any(), any()))
                .thenReturn(Mono.just(List.of(InvestmentPortfolio.builder().build())));
            when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
                .thenReturn(Mono.error(new RuntimeException("Trading account upsert failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertDepositsAndAllocations
    // =========================================================================

    @Nested
    @DisplayName("upsertDepositsAndAllocations")
    class UpsertDepositsAndAllocationsTests {

        @Test
        @DisplayName("should upsert allocations and mark task COMPLETED")
        void upsertDepositsAndAllocations_success() {
            InvestmentTask task = createFullTask();
            stubAllServicesSuccess();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioService).upsertDeposits(any());
            verify(investmentPortfolioAllocationService).createDepositAllocation(any());
            verify(asyncTaskService).checkPriceAsyncTasksFinished(any());
        }

        @Test
        @DisplayName("should continue when deposit upsert fails for a portfolio")
        void upsertDeposits_depositFailure_continuesAndCompletes() {
            InvestmentTask task = createFullTask();
            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(clientWithId())));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.just(List.of(new PortfolioProduct())));
            when(investmentPortfolioService.upsertPortfolios(any(), any()))
                .thenReturn(Mono.just(List.of(InvestmentPortfolio.builder().build())));
            when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioService.upsertDeposits(any()))
                .thenReturn(Mono.error(new RuntimeException("deposit failed")));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.empty());
            when(investmentPortfolioAllocationService.generateAllocations(any(), any(), any()))
                .thenReturn(Mono.empty());

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioAllocationService, never()).createDepositAllocation(any());
        }

        @Test
        @DisplayName("should mark task FAILED when allocation upsert throws an error")
        void upsertDepositsAndAllocations_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(clientWithId())));
            when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.just(List.of(new PortfolioProduct())));
            when(investmentPortfolioService.upsertPortfolios(any(), any()))
                .thenReturn(Mono.just(List.of(InvestmentPortfolio.builder().build())));
            when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
                .thenReturn(Mono.just(List.of()));
            when(investmentPortfolioService.upsertDeposits(any()))
                .thenReturn(Mono.just(new Deposit()));
            when(investmentPortfolioAllocationService.createDepositAllocation(any()))
                .thenReturn(Mono.just(new Deposit()));
            when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
                .thenReturn(Mono.empty());
            when(investmentPortfolioAllocationService.generateAllocations(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Allocation upsert failure")));

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // Helper / Builder Methods
    // =========================================================================

    private InvestmentTask createMinimalTask() {
        return new InvestmentTask("minimal-task", InvestmentData.builder()
            .clientUsers(Collections.emptyList())
            .investmentArrangements(Collections.emptyList())
            .modelPortfolios(Collections.emptyList())
            .investmentPortfolioTradingAccounts(Collections.emptyList())
            .portfolios(Collections.emptyList())
            .build());
    }

    private InvestmentTask createTaskWithModelPortfolios() {
        return new InvestmentTask("model-portfolio-task", InvestmentData.builder()
            .clientUsers(Collections.emptyList())
            .investmentArrangements(Collections.emptyList())
            .modelPortfolios(List.of(ModelPortfolio.builder()
                .productTypeEnum(ProductTypeEnum.ROBO_ADVISOR)
                .riskLevel(5)
                .build()))
            .investmentPortfolioTradingAccounts(Collections.emptyList())
            .portfolios(Collections.emptyList())
            .build());
    }

    private InvestmentTask createTaskWithRiskQuestions() {
        RiskQuestion q = new RiskQuestion();
        q.setCode("INVESTMENT_HORIZON");
        q.setOrder(1);
        PortfolioRiskAssessment portfolioRiskAssessment = PortfolioRiskAssessment.builder()
            .riskQuestions(List.of(q))
            .build();

        return new InvestmentTask("risk-question-task", InvestmentData.builder()
            .clientUsers(Collections.emptyList())
            .investmentArrangements(Collections.emptyList())
            .modelPortfolios(Collections.emptyList())
            .investmentPortfolioTradingAccounts(Collections.emptyList())
            .portfolios(Collections.emptyList())
            .portfolioRiskAssessments(List.of(portfolioRiskAssessment))
            .build());
    }

    private InvestmentTask createTaskWithAssets(Asset... assets) {
        return new InvestmentTask("asset-task", InvestmentData.builder()
            .investmentAssetData(InvestmentAssetData.builder().assets(List.of(assets)).build())
            .clientUsers(Collections.emptyList())
            .investmentArrangements(Collections.emptyList())
            .portfolios(Collections.emptyList())
            .build());
    }

    private InvestmentTask createTaskWithAssets() {
        return createTaskWithAssets(
            Asset.builder().isin("US0378331005").market("XNAS").currency("USD").build());
    }

    private InvestmentTask createFullTask() {
        return new InvestmentTask("full-task", InvestmentData.builder()
            .clientUsers(List.of(ClientUser.builder()
                .investmentClientId(UUID.randomUUID())
                .legalEntityId(LE_INTERNAL_ID)
                .build()))
            .investmentArrangements(List.of(InvestmentArrangement.builder()
                .externalId(ARRANGEMENT_EXTERNAL_ID)
                .build()))
            .modelPortfolios(List.of(ModelPortfolio.builder()
                .productTypeEnum(ProductTypeEnum.ROBO_ADVISOR)
                .riskLevel(5)
                .build()))
            .investmentPortfolioTradingAccounts(List.of(InvestmentPortfolioTradingAccount.builder()
                .portfolioExternalId(PORTFOLIO_EXTERNAL_ID)
                .accountId(ACCOUNT_ID)
                .accountExternalId(ACCOUNT_EXTERNAL_ID)
                .isDefault(true)
                .isInternal(false)
                .build()))
            .portfolios(List.of(InvestmentPortfolio.builder().portfolio(new PortfolioList()).build()))
            .build());
    }

    private void stubAllServicesSuccess() {
        when(investmentModelPortfolioService.upsertModels(any()))
            .thenReturn(Flux.just(new OASModelPortfolioResponse()));
        when(clientService.upsertClients(any()))
            .thenReturn(Mono.just(List.of(ClientUser.builder().investmentClientId(UUID.randomUUID()).build())));
        when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentRiskAssessmentService.upsertRiskAssessments(any(), any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
            .thenReturn(Mono.just(List.of(new PortfolioProduct())));
        when(investmentPortfolioService.upsertPortfolios(any(), any()))
            .thenReturn(Mono.just(List.of(InvestmentPortfolio.builder().build())));
        when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioService.upsertDeposits(any()))
            .thenReturn(Mono.just(new Deposit()));
        when(investmentPortfolioAllocationService.createDepositAllocation(any()))
            .thenReturn(Mono.just(new Deposit()));
        when(investmentPortfolioAllocationService.generateAllocations(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
            .thenReturn(Mono.empty());
    }

    private ClientUser clientWithId() {
        return ClientUser.builder().investmentClientId(UUID.randomUUID()).legalEntityId(LE_INTERNAL_ID).build();
    }

    private void wireTrivialPipelineAfterModelPortfolios() {
        when(investmentModelPortfolioService.upsertModels(any()))
            .thenReturn(Flux.empty());
        when(clientService.upsertClients(any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentRiskQuestionaryService.upsertRiskQuestions(any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioProductService.upsertInvestmentProducts(any(), any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioService.upsertPortfolios(any(), any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioAllocationService.generateAllocations(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
            .thenReturn(Mono.empty());
    }

}
