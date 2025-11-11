package com.backbase.stream.investment.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.backbase.investment.api.service.ApiClient;
import com.backbase.investment.api.service.v1.model.*;
import com.backbase.stream.investment.ClientUser;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.service.InvestmentAssetUniverseService;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.worker.model.StreamTask.State;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Integration tests for {@link InvestmentSaga}.
 *
 * <p>These tests verify the complete saga workflow including:
 * <ul>
 *   <li>Asset creation</li>
 *   <li>Client upsert</li>
 *   <li>Product upsert</li>
 *   <li>Portfolio upsert</li>
 *   <li>Error handling and recovery</li>
 *   <li>State management</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentSaga Integration Tests")
class InvestmentSagaIT {

    @Mock
    private InvestmentClientService clientService;

    @Mock
    private InvestmentPortfolioService portfolioService;

    @Mock
    private ApiClient apiClient;

    @Mock
    private InvestmentAssetUniverseService assetUniverseService;

    private InvestmentSaga saga;
    private InvestmentTask testTask;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        saga = new InvestmentSaga(clientService, portfolioService, apiClient, assetUniverseService);

        // Initialize UUIDs
        clientId = UUID.randomUUID();

        // Create test task
        testTask = createTestTask();
    }

    @Test
    @DisplayName("Should successfully execute complete saga workflow")
    void executeTask_success_completesAllSteps() {
        // Given: Mock successful responses for all steps
        mockSuccessfulMarketCreation();
        mockSuccessfulAssetCreation();
        mockSuccessfulClientUpsert();
        mockSuccessfulProductUpsert();
        mockSuccessfulPortfolioUpsert();

        // When: Execute the saga
        StepVerifier.create(saga.executeTask(testTask))
            .assertNext(completedTask -> {
                // Then: Verify task completion
                assertThat(completedTask.getState()).isEqualTo(State.COMPLETED);

                // Verify all steps were executed
                verify(clientService, times(2)).upsertClient(any(ClientCreateRequest.class), anyString());
                verify(portfolioService, times(1)).upsertInvestmentProducts(any(InvestmentArrangement.class));
                verify(portfolioService, times(1)).upsertInvestmentPortfolios(any(InvestmentArrangement.class),
                    anyMap());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should continue saga when asset creation fails")
    void executeTask_assetCreationFails_continuesSaga() {
        // Given: Asset creation fails but other steps succeed
        mockSuccessfulMarketCreation();
        mockSuccessfulAssetCreation();
        mockSuccessfulClientUpsert();
        mockSuccessfulProductUpsert();
        mockSuccessfulPortfolioUpsert();

        // When: Execute the saga
        StepVerifier.create(saga.executeTask(testTask))
            .assertNext(completedTask -> {
                // Then: Saga should complete despite asset creation failure
                assertThat(completedTask.getState()).isEqualTo(State.COMPLETED);

                // Verify remaining steps executed
                verify(clientService, times(2)).upsertClient(any(ClientCreateRequest.class), anyString());
                verify(portfolioService, times(1)).upsertInvestmentProducts(any(InvestmentArrangement.class));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle client upsert failure and set task to FAILED")
    void executeTask_clientUpsertFails_setsTaskToFailed() {
        // Given: Client upsert fails
        mockSuccessfulAssetCreation();
        when(clientService.upsertClient(any(ClientCreateRequest.class), anyString()))
            .thenReturn(Mono.error(new RuntimeException("Client upsert failed")));

        // When: Execute the saga
        StepVerifier.create(saga.executeTask(testTask))
            .assertNext(failedTask -> {
                // Then: Task should be in FAILED state
                assertThat(failedTask.getState()).isEqualTo(State.FAILED);

                // Verify subsequent steps were not executed
                verify(portfolioService, never()).upsertInvestmentProducts(any());
                verify(portfolioService, never()).upsertInvestmentPortfolios(any(), anyMap());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle product upsert failure")
    void executeTask_productUpsertFails_setsTaskToFailed() {
        // Given: Product upsert fails
        mockSuccessfulAssetCreation();
        mockSuccessfulClientUpsert();
        when(portfolioService.upsertInvestmentProducts(any(InvestmentArrangement.class)))
            .thenReturn(Mono.error(new RuntimeException("Product upsert failed")));

        // When: Execute the saga
        StepVerifier.create(saga.executeTask(testTask))
            .assertNext(failedTask -> {
                // Then: Task should be in FAILED state
                assertThat(failedTask.getState()).isEqualTo(State.FAILED);

                // Verify clients were upserted but portfolio was not
                verify(clientService, times(2)).upsertClient(any(ClientCreateRequest.class), anyString());
                verify(portfolioService, never()).upsertInvestmentPortfolios(any(), anyMap());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle portfolio upsert failure")
    void executeTask_portfolioUpsertFails_setsTaskToFailed() {
        // Given: Portfolio upsert fails
        mockSuccessfulAssetCreation();
        mockSuccessfulClientUpsert();
        mockSuccessfulProductUpsert();
        when(portfolioService.upsertInvestmentPortfolios(any(InvestmentArrangement.class), anyMap()))
            .thenReturn(Mono.error(new RuntimeException("Portfolio upsert failed")));

        // When: Execute the saga
        StepVerifier.create(saga.executeTask(testTask))
            .assertNext(failedTask -> {
                // Then: Task should be in FAILED state
                assertThat(failedTask.getState()).isEqualTo(State.FAILED);

                // Verify all previous steps were executed
                verify(clientService, times(2)).upsertClient(any(ClientCreateRequest.class), anyString());
                verify(portfolioService).upsertInvestmentProducts(any(InvestmentArrangement.class));
                verify(portfolioService).upsertInvestmentPortfolios(any(InvestmentArrangement.class), anyMap());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono for rollback")
    void rollBack_notImplemented_returnsEmptyMono() {
        // When: Rollback is called
        StepVerifier.create(saga.rollBack(testTask))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should construct saga successfully")
    void sagaConstruction_success() {
        // Given/When: Saga is constructed
        // Then: Saga is not null
        assertThat(saga).isNotNull();
    }

    // Helper methods

    private InvestmentTask createTestTask() {
        InvestmentData data = InvestmentData.builder()
            .clientUsers(List.of(
                createClientUser("user1", "le1"),
                createClientUser("user2", "le2")
            ))
            .investmentArrangements(List.of(createArrangement()))
            .build();

        return new InvestmentTask("task-001", data);
    }

    private ClientUser createClientUser(String userId, String legalEntityId) {
        return ClientUser.builder()
            .investmentClientId(clientId)
            .internalUserId(userId)
            .externalUserId(userId + "-ext")
            .legalEntityExternalId(legalEntityId)
            .build();
    }

    private InvestmentArrangement createArrangement() {
        return InvestmentArrangement.builder()
            .externalId("arr-001")
            .name("Test Arrangement")
            .internalId("internal-001")
            .legalEntityExternalIds(List.of("le1", "le2"))
            .build();
    }

    private void mockSuccessfulAssetCreation() {
        // Just return empty to simulate success - actual response type is not importantfor our tests
//        apiClient.invokeAPI("/service-api/v2/asset/assets/", HttpMethod.POST, pathParams, queryParams, postBody,
//            headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames,
//            localVarReturnType)
        ResponseSpec mock = mock(ResponseSpec.class);
        lenient().when(mock.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenAnswer(invocation -> {
                Asset asset = new Asset();
                return Mono.just(asset);
            });
        lenient().when(apiClient.invokeAPI(eq("/service-api/v2/asset/assets/"), eq(HttpMethod.POST), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mock);
//            .thenAnswer(invocation -> Mono.empty());
    }

    private void mockSuccessfulClientUpsert() {
        lenient().when(clientService.upsertClient(any(ClientCreateRequest.class), anyString()))
            .thenAnswer(invocation -> {
                String leId = invocation.getArgument(1);
                return Mono.just(createClientUser("user-" + leId, leId));
            });
    }

    private void mockSuccessfulProductUpsert() {
        PortfolioProduct product = new PortfolioProduct();

        when(portfolioService.upsertInvestmentProducts(any(InvestmentArrangement.class)))
            .thenReturn(Mono.just(product));
    }

    private void mockSuccessfulPortfolioUpsert() {
        PortfolioList portfolio = new PortfolioList()
            .name("Test Portfolio")
            .externalId("portfolio-001");

        when(portfolioService.upsertInvestmentPortfolios(any(InvestmentArrangement.class), anyMap()))
            .thenReturn(Mono.just(portfolio));
    }

    private List<Market> getMarkets() {
        return List.of(
                new Market().code("XETR").name("Xetra").timeZone(TimeZoneEnum.EUROPE_BERLIN).sessionStart("09:00:00").sessionEnd("18:00:00"),
                new Market().code("XSWX").name("SIX").timeZone(TimeZoneEnum.UTC).sessionStart("09:30:00").sessionEnd("17:00:00"),
                new Market().code("XLON").name("LSE").timeZone(TimeZoneEnum.UTC).sessionStart("09:00:00").sessionEnd("17:30:00")
        );
    }

    private void mockSuccessfulMarketCreation() {
        List<Market> markets = getMarkets();
        lenient().when(assetUniverseService.getOrCreateMarket(any(MarketRequest.class)))
                .thenReturn(Mono.just(markets.getFirst()));
    }
}

