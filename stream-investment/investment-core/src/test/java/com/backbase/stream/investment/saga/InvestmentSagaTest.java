package com.backbase.stream.investment.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.ClientUser;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolioTradingAccount;
import com.backbase.stream.investment.service.AsyncTaskService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
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
    private InvestmentPortfolioService investmentPortfolioService;

    @Mock
    private InvestmentPortfolioAllocationService investmentPortfolioAllocationService;

    @Mock
    private InvestmentModelPortfolioService investmentModelPortfolioService;

    @Mock
    private AsyncTaskService asyncTaskService;

    @Mock
    private InvestmentIngestionConfigurationProperties configurationProperties;

    private InvestmentSaga investmentSaga;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(configurationProperties.isWealthEnabled()).thenReturn(true);
        investmentSaga = new InvestmentSaga(
            clientService,
            investmentPortfolioService,
            investmentPortfolioAllocationService,
            investmentModelPortfolioService,
            asyncTaskService,
            configurationProperties
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
    // upsertPortfolioModels
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
            stubAllServicesSuccess();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
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
    // upsertArrangements
    // =========================================================================

    @Nested
    @DisplayName("upsertArrangements")
    class UpsertArrangementsTests {

        @Test
        @DisplayName("should complete successfully without calling service when arrangement list is empty")
        void upsertArrangements_emptyList_completesSuccessfully() {
            InvestmentTask task = createMinimalTask();
            wireTrivialPipelineAfterModelPortfolios();

            StepVerifier.create(investmentSaga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentPortfolioService).upsertInvestmentProducts(any(), any());
        }

        @Test
        @DisplayName("should mark task FAILED when arrangement upsert throws an error")
        void upsertArrangements_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(ClientUser.builder().build())));
            when(investmentPortfolioService.upsertInvestmentProducts(any(), any()))
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
        @DisplayName("should mark task FAILED when portfolio upsert throws an error")
        void upsertPortfolios_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(ClientUser.builder().build())));
            when(investmentPortfolioService.upsertInvestmentProducts(any(), any()))
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
                .thenReturn(Mono.just(List.of(ClientUser.builder().build())));
            when(investmentPortfolioService.upsertInvestmentProducts(any(), any()))
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
        }

        @Test
        @DisplayName("should mark task FAILED when allocation upsert throws an error")
        void upsertDepositsAndAllocations_error_marksTaskFailed() {
            InvestmentTask task = createFullTask();

            when(investmentModelPortfolioService.upsertModels(any()))
                .thenReturn(Flux.just(new OASModelPortfolioResponse()));
            when(clientService.upsertClients(any()))
                .thenReturn(Mono.just(List.of(ClientUser.builder().build())));
            when(investmentPortfolioService.upsertInvestmentProducts(any(), any()))
                .thenReturn(Mono.just(List.of(new PortfolioProduct())));
            when(investmentPortfolioService.upsertPortfolios(any(), any()))
                .thenReturn(Mono.just(List.of(InvestmentPortfolio.builder().build())));
            when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
                .thenReturn(Mono.empty());
            when(investmentPortfolioService.upsertDeposits(any()))
                .thenReturn(Mono.empty());
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
            .thenReturn(Mono.just(List.of(ClientUser.builder().build())));
        when(investmentPortfolioService.upsertInvestmentProducts(any(), any()))
            .thenReturn(Mono.just(List.of(new PortfolioProduct())));
        when(investmentPortfolioService.upsertPortfolios(any(), any()))
            .thenReturn(Mono.just(List.of(InvestmentPortfolio.builder().build())));
        when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
            .thenReturn(Mono.empty());
        when(investmentPortfolioService.upsertDeposits(any()))
            .thenReturn(Mono.empty());
        when(investmentPortfolioAllocationService.generateAllocations(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
            .thenReturn(Mono.empty());
    }

    private void wireTrivialPipelineAfterModelPortfolios() {
        when(investmentModelPortfolioService.upsertModels(any()))
            .thenReturn(Flux.empty());
        when(clientService.upsertClients(any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioService.upsertInvestmentProducts(any(), any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioService.upsertPortfolios(any(), any()))
            .thenReturn(Mono.just(List.of()));
        when(investmentPortfolioService.upsertPortfolioTradingAccounts(any()))
            .thenReturn(Mono.empty());
        when(investmentPortfolioAllocationService.generateAllocations(any(), any(), any()))
            .thenReturn(Mono.empty());
        when(asyncTaskService.checkPriceAsyncTasksFinished(any()))
            .thenReturn(Mono.empty());
    }

}
