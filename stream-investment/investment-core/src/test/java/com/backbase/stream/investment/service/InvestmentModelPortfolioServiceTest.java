package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.investment.api.service.v1.model.PaginatedOASModelPortfolioResponseList;
import com.backbase.stream.investment.Allocation;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestModelPortfolioService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit test suite for {@link InvestmentModelPortfolioService}.
 *
 * <p>Mocked dependencies:
 * <ul>
 *   <li>{@link FinancialAdviceApi} – list model portfolios</li>
 *   <li>{@link InvestmentRestModelPortfolioService} – create / patch model portfolios</li>
 * </ul>
 *
 * <p>Note: mapping from {@link ModelPortfolio} to the OAS request DTO is performed inside
 * {@link InvestmentRestModelPortfolioService} via {@code RestTemplateModelPortfolioMapper}
 * and is therefore not tested here.
 */
class InvestmentModelPortfolioServiceTest {

    @Mock
    private FinancialAdviceApi financialAdviceApi;

    @Mock
    private InvestmentRestModelPortfolioService investmentRestModelPortfolioService;

    private InvestmentModelPortfolioService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InvestmentModelPortfolioService(financialAdviceApi, investmentRestModelPortfolioService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // =========================================================================
    // upsertModels
    // =========================================================================

    @Nested
    @DisplayName("upsertModels")
    class UpsertModelsTests {

        @Test
        @DisplayName("should emit nothing and call no API when modelPortfolios is null")
        void upsertModels_nullModelPortfolios_emitsNothing() {
            InvestmentData data = InvestmentData.builder().modelPortfolios(null).build();

            StepVerifier.create(service.upsertModels(data)).verifyComplete();

            verify(financialAdviceApi, never()).listModelPortfolio(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
            verify(investmentRestModelPortfolioService, never()).createModelPortfolio(any());
        }

        @Test
        @DisplayName("should emit nothing and call no API when modelPortfolios is empty")
        void upsertModels_emptyModelPortfolios_emitsNothing() {
            InvestmentData data = InvestmentData.builder().modelPortfolios(Collections.emptyList()).build();

            StepVerifier.create(service.upsertModels(data)).verifyComplete();

            verify(financialAdviceApi, never()).listModelPortfolio(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should create new model portfolio and set UUID on template when none exists")
        void upsertModels_singlePortfolio_noExisting_createsAndSetsUuid() {
            UUID expectedUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Conservative", 3, 0.1);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Conservative", 3);
            OASModelPortfolioResponse created = buildResponse(expectedUuid, "Conservative", 3);
            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(Mono.just(created));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(response -> {
                    assertThat(response.getUuid()).isEqualTo(expectedUuid);
                    assertThat(response.getName()).isEqualTo("Conservative");
                })
                .verifyComplete();

            assertThat(template.getUuid()).isEqualTo(expectedUuid);
        }

        @Test
        @DisplayName("should patch existing model portfolio when one match is found")
        void upsertModels_singlePortfolio_existingFound_patches() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Balanced", 5, 0.2);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Balanced", 5);
            stubListReturnsOne("Balanced", 5, existing);

            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Balanced", 5);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                eq(existingUuid.toString()), any(ModelPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(response -> assertThat(response.getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(investmentRestModelPortfolioService, never()).createModelPortfolio(any());
        }

        @Test
        @DisplayName("should process all model portfolios and emit one response per entry")
        void upsertModels_multiplePortfolios_processesAll() {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            ModelPortfolio t1 = buildModelPortfolio("Conservative", 3, 0.1);
            ModelPortfolio t2 = buildModelPortfolio("Aggressive", 8, 0.05);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(t1, t2)).build();

            stubListReturnsEmpty("Conservative", 3);
            stubListReturnsEmpty("Aggressive", 8);

            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(
                    Mono.just(buildResponse(uuid1, "Conservative", 3)),
                    Mono.just(buildResponse(uuid2, "Aggressive", 8)));

            StepVerifier.create(service.upsertModels(data).collectList())
                .assertNext(responses -> {
                    assertThat(responses).hasSize(2);
                    assertThat(responses).extracting(OASModelPortfolioResponse::getUuid)
                        .containsExactlyInAnyOrder(uuid1, uuid2);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should propagate error from create when API call fails")
        void upsertModels_createFails_propagatesError() {
            ModelPortfolio template = buildModelPortfolio("Conservative", 3, 0.1);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Conservative", 3);
            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            StepVerifier.create(service.upsertModels(data))
                .expectError(RuntimeException.class)
                .verify();
        }

        @Test
        @DisplayName("should pass the correct ModelPortfolio to the rest service on create")
        void upsertModels_passesCorrectModelPortfolioToRestService() {
            UUID expectedUuid = UUID.randomUUID();
            ModelAsset asset = new ModelAsset("US1234567890", "XNAS", "USD");
            Allocation allocation = new Allocation(asset, 0.75);
            ModelPortfolio template = ModelPortfolio.builder()
                .name("Growth").riskLevel(7).cashWeight(0.25).allocations(List.of(allocation)).build();
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Growth", 7);

            ArgumentCaptor<ModelPortfolio> requestCaptor = ArgumentCaptor.forClass(ModelPortfolio.class);
            when(investmentRestModelPortfolioService.createModelPortfolio(requestCaptor.capture()))
                .thenReturn(Mono.just(buildResponse(expectedUuid, "Growth", 7)));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(expectedUuid))
                .verifyComplete();

            ModelPortfolio captured = requestCaptor.getValue();
            assertThat(captured.getName()).isEqualTo("Growth");
            assertThat(captured.getRiskLevel()).isEqualTo(7);
            assertThat(captured.getCashWeight()).isEqualTo(0.25);
            assertThat(captured.getAllocations()).hasSize(1);
            assertThat(captured.getAllocations().getFirst().weight()).isEqualTo(0.75);
            assertThat(captured.getAllocations().getFirst().asset().getIsin()).isEqualTo("US1234567890");
            assertThat(captured.getAllocations().getFirst().asset().getMarket()).isEqualTo("XNAS");
            assertThat(captured.getAllocations().getFirst().asset().getCurrency()).isEqualTo("USD");
        }
    }

    // =========================================================================
    // listExistingModelPortfolios
    // =========================================================================

    @Nested
    @DisplayName("listExistingModelPortfolios")
    class ListExistingModelPortfoliosTests {

        @Test
        @DisplayName("should create new portfolio when list response has empty results")
        void listExisting_emptyResults_createsNew() {
            UUID expectedUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Conservative", 2, 0.15);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Conservative", 2);
            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(Mono.just(buildResponse(expectedUuid, "Conservative", 2)));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(expectedUuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("should patch using first result when exactly one match is found")
        void listExisting_oneResult_patchesFirstResult() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Moderate", 5, 0.3);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Moderate", 5);
            stubListReturnsOne("Moderate", 5, existing);
            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Moderate", 5);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                eq(existingUuid.toString()), any(ModelPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(investmentRestModelPortfolioService, never()).createModelPortfolio(any());
        }

        @Test
        @DisplayName("should patch first result and not create when multiple matches are found")
        void listExisting_multipleResults_patchesFirstResult() {
            UUID firstUuid = UUID.randomUUID();
            UUID secondUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Balanced", 6, 0.2);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse first = buildResponse(firstUuid, "Balanced", 6);
            OASModelPortfolioResponse second = buildResponse(secondUuid, "Balanced", 6);
            PaginatedOASModelPortfolioResponseList page = new PaginatedOASModelPortfolioResponseList()
                .count(2).results(List.of(first, second));
            when(financialAdviceApi.listModelPortfolio(
                isNull(), isNull(), isNull(), eq(1), eq("Balanced"),
                isNull(), isNull(), isNull(), eq(6), isNull()))
                .thenReturn(Mono.just(page));

            OASModelPortfolioResponse patched = buildResponse(firstUuid, "Balanced", 6);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                eq(firstUuid.toString()), any(ModelPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(firstUuid))
                .verifyComplete();

            verify(investmentRestModelPortfolioService, never()).createModelPortfolio(any());
        }

        @Test
        @DisplayName("should propagate error when listModelPortfolio API call fails")
        void listExisting_apiError_propagatesError() {
            ModelPortfolio template = buildModelPortfolio("Balanced", 5, 0.2);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            when(financialAdviceApi.listModelPortfolio(
                isNull(), isNull(), isNull(), eq(1), eq("Balanced"),
                isNull(), isNull(), isNull(), eq(5), isNull()))
                .thenReturn(Mono.error(new RuntimeException("list API failed")));

            StepVerifier.create(service.upsertModels(data))
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    // =========================================================================
    // createNewModelPortfolio
    // =========================================================================

    @Nested
    @DisplayName("createNewModelPortfolio")
    class CreateNewModelPortfolioTests {

        @Test
        @DisplayName("should return created portfolio response on successful create")
        void createNew_success_returnsCreatedResponse() {
            UUID newUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Income", 2, 0.4);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Income", 2);
            OASModelPortfolioResponse created = buildResponse(newUuid, "Income", 2);
            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(Mono.just(created));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> {
                    assertThat(r.getUuid()).isEqualTo(newUuid);
                    assertThat(r.getName()).isEqualTo("Income");
                    assertThat(r.getRiskLevel()).isEqualTo(2);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should propagate WebClientResponseException from create API")
        void createNew_webClientResponseException_propagatesError() {
            ModelPortfolio template = buildModelPortfolio("Income", 2, 0.4);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Income", 2);
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", null, null, null);
            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(Mono.error(ex));

            StepVerifier.create(service.upsertModels(data))
                .expectError(WebClientResponseException.class)
                .verify();
        }

        @Test
        @DisplayName("should propagate generic exception from create API")
        void createNew_genericException_propagatesError() {
            ModelPortfolio template = buildModelPortfolio("Income", 2, 0.4);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            stubListReturnsEmpty("Income", 2);
            when(investmentRestModelPortfolioService.createModelPortfolio(any(ModelPortfolio.class)))
                .thenReturn(Mono.error(new IllegalStateException("unexpected")));

            StepVerifier.create(service.upsertModels(data))
                .expectError(IllegalStateException.class)
                .verify();
        }
    }

    // =========================================================================
    // patchModelPortfolio
    // =========================================================================

    @Nested
    @DisplayName("patchModelPortfolio")
    class PatchModelPortfolioTests {

        @Test
        @DisplayName("should return patched portfolio response on successful patch")
        void patch_success_returnsPatchedResponse() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Dynamic", 9, 0.05);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Dynamic", 9);
            stubListReturnsOne("Dynamic", 9, existing);
            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Dynamic", 9);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                eq(existingUuid.toString()), any(ModelPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> {
                    assertThat(r.getUuid()).isEqualTo(existingUuid);
                    assertThat(r.getName()).isEqualTo("Dynamic");
                    assertThat(r.getRiskLevel()).isEqualTo(9);
                })
                .verifyComplete();

            verify(investmentRestModelPortfolioService, never()).createModelPortfolio(any());
        }

        @Test
        @DisplayName("should call patch API with the UUID from the existing portfolio")
        void patch_usesCorrectUuidFromExistingPortfolio() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Stable", 4, 0.3);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Stable", 4);
            stubListReturnsOne("Stable", 4, existing);

            ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Stable", 4);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                uuidCaptor.capture(), any(ModelPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            assertThat(uuidCaptor.getValue()).isEqualTo(existingUuid.toString());
        }

        @Test
        @DisplayName("should propagate WebClientResponseException from patch API")
        void patch_webClientResponseException_propagatesError() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Dynamic", 9, 0.05);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Dynamic", 9);
            stubListReturnsOne("Dynamic", 9, existing);
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                eq(existingUuid.toString()), any(ModelPortfolio.class)))
                .thenReturn(Mono.error(ex));

            StepVerifier.create(service.upsertModels(data))
                .expectError(WebClientResponseException.class)
                .verify();
        }

        @Test
        @DisplayName("should propagate generic exception from patch API")
        void patch_genericException_propagatesError() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Dynamic", 9, 0.05);
            InvestmentData data = InvestmentData.builder().modelPortfolios(List.of(template)).build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Dynamic", 9);
            stubListReturnsOne("Dynamic", 9, existing);
            when(investmentRestModelPortfolioService.patchModelPortfolio(
                eq(existingUuid.toString()), any(ModelPortfolio.class)))
                .thenReturn(Mono.error(new RuntimeException("patch failed")));

            StepVerifier.create(service.upsertModels(data))
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ModelPortfolio buildModelPortfolio(String name, int riskLevel, double cashWeight) {
        ModelAsset asset = new ModelAsset("US0378331005", "XNAS", "USD");
        Allocation allocation = new Allocation(asset, 1.0 - cashWeight);
        return ModelPortfolio.builder()
            .name(name).riskLevel(riskLevel).cashWeight(cashWeight).allocations(List.of(allocation)).build();
    }

    private OASModelPortfolioResponse buildResponse(UUID uuid, String name, int riskLevel) {
        OASModelPortfolioResponse response = new OASModelPortfolioResponse(uuid);
        response.setName(name);
        response.setRiskLevel(riskLevel);
        return response;
    }

    private void stubListReturnsEmpty(String name, int riskLevel) {
        PaginatedOASModelPortfolioResponseList emptyPage = new PaginatedOASModelPortfolioResponseList()
            .count(0).results(Collections.emptyList());
        when(financialAdviceApi.listModelPortfolio(
            any(), any(), any(), eq(1), eq(name), any(), any(), any(), eq(riskLevel), any()))
            .thenReturn(Mono.just(emptyPage));
    }

    private void stubListReturnsOne(String name, int riskLevel, OASModelPortfolioResponse response) {
        PaginatedOASModelPortfolioResponseList page = new PaginatedOASModelPortfolioResponseList()
            .count(1).results(List.of(response));
        when(financialAdviceApi.listModelPortfolio(
            any(), any(), any(), eq(1), eq(name), any(), any(), any(), eq(riskLevel), any()))
            .thenReturn(Mono.just(page));
    }
}

