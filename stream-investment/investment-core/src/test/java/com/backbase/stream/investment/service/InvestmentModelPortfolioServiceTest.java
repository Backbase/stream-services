package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.FinancialAdviceApi;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioRequestDataRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.investment.api.service.v1.model.PaginatedOASModelPortfolioResponseList;
import com.backbase.stream.investment.Allocation;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.ModelPortfolio;
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
 * <p>This class verifies the complete orchestration logic of the service, which drives
 * the model portfolio ingestion pipeline through the following stages:
 * <ol>
 *   <li>List existing model portfolios by name and risk level</li>
 *   <li>If found, patch the existing model portfolio</li>
 *   <li>If not found, create a new model portfolio</li>
 * </ol>
 *
 * <p>Test strategy:
 * <ul>
 *   <li>Each pipeline stage is tested in isolation via a dedicated {@code @Nested} class.</li>
 *   <li>Happy-path, empty-collection, null-field, and error scenarios are covered for every stage.</li>
 *   <li>All reactive assertions use Project Reactor's {@link StepVerifier}.</li>
 * </ul>
 *
 * <p>Mocked dependencies:
 * <ul>
 *   <li>{@link FinancialAdviceApi} – list / create / patch model portfolios</li>
 * </ul>
 */
class InvestmentModelPortfolioServiceTest {

    @Mock
    private FinancialAdviceApi financialAdviceApi;

    private InvestmentModelPortfolioService service;

    private AutoCloseable mocks;

    /**
     * Opens Mockito annotations and constructs the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InvestmentModelPortfolioService(financialAdviceApi);
    }

    /**
     * Closes Mockito mocks after each test to release resources.
     */
    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // =========================================================================
    // upsertModels – top-level flux orchestration
    // =========================================================================

    /**
     * Tests for the {@link InvestmentModelPortfolioService#upsertModels} entry-point
     * that iterates over the model portfolio list in {@link InvestmentData}.
     */
    @Nested
    @DisplayName("upsertModels")
    class UpsertModelsTests {

        /**
         * Verifies that when {@code investmentData.getModelPortfolios()} is {@code null},
         * {@code Objects.requireNonNullElse} substitutes an empty list and the flux
         * completes without emitting any items or calling any downstream API.
         */
        @Test
        @DisplayName("should emit nothing and call no API when modelPortfolios is null")
        void upsertModels_nullModelPortfolios_emitsNothing() {
            // Arrange
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(null)
                .build();

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .verifyComplete();

            verify(financialAdviceApi, never()).listModelPortfolio(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
            verify(financialAdviceApi, never()).createModelPortfolio(
                any(), any(), any(), any(), any());
        }

        /**
         * Verifies that when {@code investmentData.getModelPortfolios()} is an empty list,
         * the flux completes without emitting any items or calling any downstream API.
         */
        @Test
        @DisplayName("should emit nothing and call no API when modelPortfolios is empty")
        void upsertModels_emptyModelPortfolios_emitsNothing() {
            // Arrange
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(Collections.emptyList())
                .build();

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .verifyComplete();

            verify(financialAdviceApi, never()).listModelPortfolio(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        /**
         * Verifies that a single model portfolio with allocations is processed correctly:
         * the allocation is mapped to {@code OASAssetModelPortfolioRequestRequest},
         * no existing portfolio is found, a new one is created, and the returned UUID
         * is set on the template.
         */
        @Test
        @DisplayName("should create new model portfolio and set UUID on template when none exists")
        void upsertModels_singlePortfolio_noExisting_createsAndSetsUuid() {
            // Arrange
            UUID expectedUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Conservative", 3, 0.1);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Conservative", 3);
            OASModelPortfolioResponse created = buildResponse(expectedUuid, "Conservative", 3);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(response -> {
                    assertThat(response.getUuid()).isEqualTo(expectedUuid);
                    assertThat(response.getName()).isEqualTo("Conservative");
                })
                .verifyComplete();

            assertThat(template.getUuid()).isEqualTo(expectedUuid);
        }

        /**
         * Verifies that when an existing model portfolio is found (by name and risk level),
         * the service patches it instead of creating a new one, and returns the patched response.
         */
        @Test
        @DisplayName("should patch existing model portfolio when one match is found")
        void upsertModels_singlePortfolio_existingFound_patches() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Balanced", 5, 0.2);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Balanced", 5);
            stubListReturnsOne("Balanced", 5, existing);

            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Balanced", 5);
            when(financialAdviceApi.patchModelPortfolio(
                eq(existingUuid.toString()), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(response -> assertThat(response.getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(financialAdviceApi, never()).createModelPortfolio(
                any(), any(), any(), any(), any());
        }

        /**
         * Verifies that when multiple model portfolios exist in the list, all are processed
         * and each emits a response, so the flux emits one item per input portfolio.
         * Uses {@code collectList()} to avoid ordering dependency from {@code flatMap}.
         */
        @Test
        @DisplayName("should process all model portfolios and emit one response per entry")
        void upsertModels_multiplePortfolios_processesAll() {
            // Arrange
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            ModelPortfolio t1 = buildModelPortfolio("Conservative", 3, 0.1);
            ModelPortfolio t2 = buildModelPortfolio("Aggressive", 8, 0.05);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(t1, t2))
                .build();

            stubListReturnsEmpty("Conservative", 3);
            stubListReturnsEmpty("Aggressive", 8);

            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(
                    Mono.just(buildResponse(uuid1, "Conservative", 3)),
                    Mono.just(buildResponse(uuid2, "Aggressive", 8)));

            // Act & Assert — collect to list so ordering from flatMap does not matter
            StepVerifier.create(service.upsertModels(data).collectList())
                .assertNext(responses -> {
                    assertThat(responses).hasSize(2);
                    assertThat(responses).extracting(OASModelPortfolioResponse::getUuid)
                        .containsExactlyInAnyOrder(uuid1, uuid2);
                })
                .verifyComplete();
        }

        /**
         * Verifies that when the downstream create call fails, the error is propagated
         * through the flux to the subscriber.
         */
        @Test
        @DisplayName("should propagate error from create when API call fails")
        void upsertModels_createFails_propagatesError() {
            // Arrange
            ModelPortfolio template = buildModelPortfolio("Conservative", 3, 0.1);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Conservative", 3);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .expectError(RuntimeException.class)
                .verify();
        }

        /**
         * Verifies that allocation fields (asset map, weight) from the {@link ModelPortfolio}
         * template are correctly mapped into the {@link OASModelPortfolioRequestDataRequest}
         * sent to the create API.
         */
        @Test
        @DisplayName("should correctly map allocation and cashWeight from template to API request")
        void upsertModels_allocationMapping_correctlyMapsFields() {
            // Arrange
            UUID expectedUuid = UUID.randomUUID();
            ModelAsset asset = new ModelAsset("US1234567890", "XNAS", "USD");
            Allocation allocation = new Allocation(asset, 0.75);
            ModelPortfolio template = ModelPortfolio.builder()
                .name("Growth")
                .riskLevel(7)
                .cashWeight(0.25)
                .allocations(List.of(allocation))
                .build();
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Growth", 7);

            ArgumentCaptor<OASModelPortfolioRequestDataRequest> requestCaptor =
                ArgumentCaptor.forClass(OASModelPortfolioRequestDataRequest.class);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), requestCaptor.capture(), isNull()))
                .thenReturn(Mono.just(buildResponse(expectedUuid, "Growth", 7)));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(expectedUuid))
                .verifyComplete();

            OASModelPortfolioRequestDataRequest captured = requestCaptor.getValue();
            assertThat(captured.getName()).isEqualTo("Growth");
            assertThat(captured.getRiskLevel()).isEqualTo(7);
            assertThat(captured.getCashWeight()).isEqualTo(0.25);
            assertThat(captured.getAllocation()).hasSize(1);
            assertThat(captured.getAllocation().getFirst().getWeight()).isEqualTo(0.75);
            assertThat(captured.getAllocation().getFirst().getAsset())
                .containsEntry("isin", "US1234567890")
                .containsEntry("market", "XNAS")
                .containsEntry("currency", "USD");
        }
    }

    // =========================================================================
    // listExistingModelPortfolios – internal lookup branch logic
    // =========================================================================

    /**
     * Tests for the internal {@code listExistingModelPortfolios} logic, exercised
     * indirectly through {@code upsertModels}.
     */
    @Nested
    @DisplayName("listExistingModelPortfolios")
    class ListExistingModelPortfoliosTests {

        /**
         * Verifies that when the API returns a {@code PaginatedOASModelPortfolioResponseList}
         * with an empty results list, the code treats it as "not found" and creates new.
         */
        @Test
        @DisplayName("should create new portfolio when list response has empty results")
        void listExisting_emptyResults_createsNew() {
            // Arrange
            UUID expectedUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Conservative", 2, 0.15);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Conservative", 2);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(buildResponse(expectedUuid, "Conservative", 2)));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(expectedUuid))
                .verifyComplete();
        }

        /**
         * Verifies that when the list API returns exactly one match, the first (and only)
         * result is used to patch, and create is never called.
         */
        @Test
        @DisplayName("should patch using first result when exactly one match is found")
        void listExisting_oneResult_patchesFirstResult() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Moderate", 5, 0.3);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Moderate", 5);
            stubListReturnsOne("Moderate", 5, existing);
            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Moderate", 5);
            when(financialAdviceApi.patchModelPortfolio(
                eq(existingUuid.toString()), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(financialAdviceApi, never()).createModelPortfolio(
                any(), any(), any(), any(), any());
        }

        /**
         * Verifies that when the list API returns more than one match, the first result
         * is still used to patch (warn-and-use-first behaviour) and create is never called.
         */
        @Test
        @DisplayName("should patch first result and not create when multiple matches are found")
        void listExisting_multipleResults_patchesFirstResult() {
            // Arrange
            UUID firstUuid = UUID.randomUUID();
            UUID secondUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Balanced", 6, 0.2);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse first = buildResponse(firstUuid, "Balanced", 6);
            OASModelPortfolioResponse second = buildResponse(secondUuid, "Balanced", 6);
            PaginatedOASModelPortfolioResponseList page =
                new PaginatedOASModelPortfolioResponseList()
                    .count(2)
                    .results(List.of(first, second));
            when(financialAdviceApi.listModelPortfolio(
                isNull(), isNull(), isNull(), eq(1), eq("Balanced"),
                isNull(), isNull(), isNull(), eq(6), isNull()))
                .thenReturn(Mono.just(page));

            OASModelPortfolioResponse patched = buildResponse(firstUuid, "Balanced", 6);
            when(financialAdviceApi.patchModelPortfolio(
                eq(firstUuid.toString()), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(firstUuid))
                .verifyComplete();

            verify(financialAdviceApi, never()).createModelPortfolio(
                any(), any(), any(), any(), any());
        }

        /**
         * Verifies that when the list API call itself fails with a reactive error,
         * the error is propagated to the subscriber.
         */
        @Test
        @DisplayName("should propagate error when listModelPortfolio API call fails")
        void listExisting_apiError_propagatesError() {
            // Arrange
            ModelPortfolio template = buildModelPortfolio("Balanced", 5, 0.2);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            when(financialAdviceApi.listModelPortfolio(
                isNull(), isNull(), isNull(), eq(1), eq("Balanced"),
                isNull(), isNull(), isNull(), eq(5), isNull()))
                .thenReturn(Mono.error(new RuntimeException("list API failed")));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    // =========================================================================
    // createNewModelPortfolio – create path
    // =========================================================================

    /**
     * Tests for the internal {@code createNewModelPortfolio} logic, exercised
     * indirectly through {@code upsertModels} when no existing portfolio is found.
     */
    @Nested
    @DisplayName("createNewModelPortfolio")
    class CreateNewModelPortfolioTests {

        /**
         * Verifies that a successful create call returns the newly created response,
         * which carries the server-assigned UUID.
         */
        @Test
        @DisplayName("should return created portfolio response on successful create")
        void createNew_success_returnsCreatedResponse() {
            // Arrange
            UUID newUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Income", 2, 0.4);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Income", 2);
            OASModelPortfolioResponse created = buildResponse(newUuid, "Income", 2);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> {
                    assertThat(r.getUuid()).isEqualTo(newUuid);
                    assertThat(r.getName()).isEqualTo("Income");
                    assertThat(r.getRiskLevel()).isEqualTo(2);
                })
                .verifyComplete();
        }

        /**
         * Verifies that a {@link WebClientResponseException} thrown by the create API
         * is propagated as a reactive error signal to the subscriber.
         */
        @Test
        @DisplayName("should propagate WebClientResponseException from create API")
        void createNew_webClientResponseException_propagatesError() {
            // Arrange
            ModelPortfolio template = buildModelPortfolio("Income", 2, 0.4);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Income", 2);
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", null, null, null);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.error(ex));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .expectError(WebClientResponseException.class)
                .verify();
        }

        /**
         * Verifies that a generic (non-WebClient) exception from the create API is also
         * propagated without wrapping.
         */
        @Test
        @DisplayName("should propagate generic exception from create API")
        void createNew_genericException_propagatesError() {
            // Arrange
            ModelPortfolio template = buildModelPortfolio("Income", 2, 0.4);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            stubListReturnsEmpty("Income", 2);
            when(financialAdviceApi.createModelPortfolio(
                isNull(), isNull(), isNull(), any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.error(new IllegalStateException("unexpected")));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .expectError(IllegalStateException.class)
                .verify();
        }
    }

    // =========================================================================
    // patchModelPortfolio – patch path
    // =========================================================================

    /**
     * Tests for the internal {@code patchModelPortfolio} logic, exercised
     * indirectly through {@code upsertModels} when an existing portfolio is found.
     */
    @Nested
    @DisplayName("patchModelPortfolio")
    class PatchModelPortfolioTests {

        /**
         * Verifies that a successful patch call returns the patched response and
         * the create API is never called.
         */
        @Test
        @DisplayName("should return patched portfolio response on successful patch")
        void patch_success_returnsPatchedResponse() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Dynamic", 9, 0.05);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Dynamic", 9);
            stubListReturnsOne("Dynamic", 9, existing);
            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Dynamic", 9);
            when(financialAdviceApi.patchModelPortfolio(
                eq(existingUuid.toString()), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> {
                    assertThat(r.getUuid()).isEqualTo(existingUuid);
                    assertThat(r.getName()).isEqualTo("Dynamic");
                    assertThat(r.getRiskLevel()).isEqualTo(9);
                })
                .verifyComplete();

            verify(financialAdviceApi, never()).createModelPortfolio(
                any(), any(), any(), any(), any());
        }

        /**
         * Verifies that the correct existing UUID is used when calling the patch API.
         */
        @Test
        @DisplayName("should call patch API with the UUID from the existing portfolio")
        void patch_usesCorrectUuidFromExistingPortfolio() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Stable", 4, 0.3);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Stable", 4);
            stubListReturnsOne("Stable", 4, existing);

            ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
            OASModelPortfolioResponse patched = buildResponse(existingUuid, "Stable", 4);
            when(financialAdviceApi.patchModelPortfolio(
                uuidCaptor.capture(), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .assertNext(r -> assertThat(r.getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            assertThat(uuidCaptor.getValue()).isEqualTo(existingUuid.toString());
        }

        /**
         * Verifies that a {@link WebClientResponseException} thrown by the patch API
         * is propagated as a reactive error signal to the subscriber.
         */
        @Test
        @DisplayName("should propagate WebClientResponseException from patch API")
        void patch_webClientResponseException_propagatesError() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Dynamic", 9, 0.05);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Dynamic", 9);
            stubListReturnsOne("Dynamic", 9, existing);
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null);
            when(financialAdviceApi.patchModelPortfolio(
                eq(existingUuid.toString()), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.error(ex));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .expectError(WebClientResponseException.class)
                .verify();
        }

        /**
         * Verifies that a generic (non-WebClient) exception from the patch API is also
         * propagated without wrapping.
         */
        @Test
        @DisplayName("should propagate generic exception from patch API")
        void patch_genericException_propagatesError() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio template = buildModelPortfolio("Dynamic", 9, 0.05);
            InvestmentData data = InvestmentData.builder()
                .modelPortfolios(List.of(template))
                .build();

            OASModelPortfolioResponse existing = buildResponse(existingUuid, "Dynamic", 9);
            stubListReturnsOne("Dynamic", 9, existing);
            when(financialAdviceApi.patchModelPortfolio(
                eq(existingUuid.toString()), isNull(), isNull(), isNull(),
                any(OASModelPortfolioRequestDataRequest.class), isNull()))
                .thenReturn(Mono.error(new RuntimeException("patch failed")));

            // Act & Assert
            StepVerifier.create(service.upsertModels(data))
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    // =========================================================================
    // Helper / Builder Methods
    // =========================================================================

    /**
     * Builds a {@link ModelPortfolio} with a single allocation for use across tests.
     *
     * @param name       portfolio name
     * @param riskLevel  portfolio risk level
     * @param cashWeight portfolio cash weight
     * @return a fully populated {@link ModelPortfolio} instance
     */
    private ModelPortfolio buildModelPortfolio(String name, int riskLevel, double cashWeight) {
        ModelAsset asset = new ModelAsset("US0378331005", "XNAS", "USD");
        Allocation allocation = new Allocation(asset, 1.0 - cashWeight);
        return ModelPortfolio.builder()
            .name(name)
            .riskLevel(riskLevel)
            .cashWeight(cashWeight)
            .allocations(List.of(allocation))
            .build();
    }

    /**
     * Builds an {@link OASModelPortfolioResponse} with the given UUID, name and risk level.
     *
     * @param uuid      server-assigned UUID
     * @param name      portfolio name
     * @param riskLevel portfolio risk level
     * @return a populated {@link OASModelPortfolioResponse}
     */
    private OASModelPortfolioResponse buildResponse(UUID uuid, String name, int riskLevel) {
        OASModelPortfolioResponse response = new OASModelPortfolioResponse(uuid);
        response.setName(name);
        response.setRiskLevel(riskLevel);
        return response;
    }

    /**
     * Stubs {@link FinancialAdviceApi#listModelPortfolio} to return an empty result page
     * for the given name and risk level.
     *
     * @param name      the portfolio name to match
     * @param riskLevel the risk level to match
     */
    private void stubListReturnsEmpty(String name, int riskLevel) {
        PaginatedOASModelPortfolioResponseList emptyPage =
            new PaginatedOASModelPortfolioResponseList()
                .count(0)
                .results(Collections.emptyList());
        when(financialAdviceApi.listModelPortfolio(
            any(), any(), any(), eq(1), eq(name),
            any(), any(), any(), eq(riskLevel), any()))
            .thenReturn(Mono.just(emptyPage));
    }

    /**
     * Stubs {@link FinancialAdviceApi#listModelPortfolio} to return a page with exactly
     * one result for the given name and risk level.
     *
     * @param name      the portfolio name to match
     * @param riskLevel the risk level to match
     * @param response  the single result to return
     */
    private void stubListReturnsOne(String name, int riskLevel, OASModelPortfolioResponse response) {
        PaginatedOASModelPortfolioResponseList page =
            new PaginatedOASModelPortfolioResponseList()
                .count(1)
                .results(List.of(response));
        when(financialAdviceApi.listModelPortfolio(
            any(), any(), any(), eq(1), eq(name),
            any(), any(), any(), eq(riskLevel), any()))
            .thenReturn(Mono.just(page));
    }
}

