package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.investment.api.service.v1.model.Assessment;
import com.backbase.investment.api.service.v1.model.BaseAssessmentRequest;
import com.backbase.investment.api.service.v1.model.OASBaseAssessment;
import com.backbase.investment.api.service.v1.model.PaginatedAssessmentList;
import com.backbase.investment.api.service.v1.model.PatchedBaseAssessmentRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit test suite for {@link InvestmentRiskAssessmentService}.
 *
 * <p>Covers the upsert pattern:
 * <ul>
 *   <li>Empty input → empty result, no API calls</li>
 *   <li>No existing assessment → create path</li>
 *   <li>Existing assessment → patch path</li>
 *   <li>Multiple assessments → each is independently upserted</li>
 *   <li>API errors → propagated to caller</li>
 * </ul>
 */
class InvestmentRiskAssessmentServiceTest {

    @Mock
    private RiskAssessmentApi riskAssessmentApi;

    private InvestmentRiskAssessmentService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InvestmentRiskAssessmentService(riskAssessmentApi);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // =========================================================================
    // upsertRiskAssessments – empty input
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskAssessments – empty input")
    class EmptyInputTests {

        @Test
        @DisplayName("should return empty list and skip API calls when assessment list is empty")
        void emptyList_returnsEmptyListAndSkipsApiCalls() {
            StepVerifier.create(service.upsertRiskAssessments("client-uuid", List.of()))
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();

            verify(riskAssessmentApi, never()).getRiskAssessments(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should treat null assessment list as empty")
        void nullList_treatedAsEmpty() {
            StepVerifier.create(service.upsertRiskAssessments("client-uuid", null))
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertRiskAssessments – create path
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskAssessments – create path")
    class CreatePathTests {

        @Test
        @DisplayName("should create a new assessment when none exists for the client")
        void noExistingAssessment_createsNew() {
            String clientUuid = UUID.randomUUID().toString();
            UUID createdUuid = UUID.randomUUID();

            PaginatedAssessmentList emptyPage = new PaginatedAssessmentList().results(List.of());
            // Use @JsonCreator constructor: (uuid, suitable, riskLevel, created, updated, flatChoices, flatRiskLevel, flatSuitable)
            OASBaseAssessment created = new OASBaseAssessment(createdUuid, null, null, null, null, null, null, null);

            when(riskAssessmentApi.getRiskAssessments(eq(clientUuid), any(), any(), any()))
                .thenReturn(Mono.just(emptyPage));
            when(riskAssessmentApi.createRiskAssessment(eq(clientUuid), any()))
                .thenReturn(Mono.just(created));

            StepVerifier.create(service.upsertRiskAssessments(clientUuid, List.of(new BaseAssessmentRequest())))
                .assertNext(result -> {
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).getUuid()).isEqualTo(createdUuid);
                })
                .verifyComplete();

            verify(riskAssessmentApi).createRiskAssessment(eq(clientUuid), any());
            verify(riskAssessmentApi, never()).patchRiskAssessment(any(), any(), any());
        }
    }

    // =========================================================================
    // upsertRiskAssessments – patch path
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskAssessments – patch path")
    class PatchPathTests {

        @Test
        @DisplayName("should patch the existing assessment when one is found for the client")
        void existingAssessment_patchesExisting() {
            String clientUuid = UUID.randomUUID().toString();
            UUID existingUuid = UUID.randomUUID();

            // Use @JsonCreator constructor: (uuid, suitable, riskLevel, created, updated, flatChoices, flatRiskLevel, flatSuitable)
            Assessment existing = new Assessment(existingUuid, null, null, null, null, null, null, null);
            PaginatedAssessmentList page = new PaginatedAssessmentList().results(List.of(existing));
            OASBaseAssessment patched = new OASBaseAssessment(existingUuid, null, null, null, null, null, null, null);

            when(riskAssessmentApi.getRiskAssessments(eq(clientUuid), any(), any(), any()))
                .thenReturn(Mono.just(page));
            when(riskAssessmentApi.patchRiskAssessment(eq(clientUuid), eq(existingUuid), any()))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertRiskAssessments(clientUuid, List.of(new BaseAssessmentRequest())))
                .assertNext(result -> {
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).getUuid()).isEqualTo(existingUuid);
                })
                .verifyComplete();

            verify(riskAssessmentApi).patchRiskAssessment(eq(clientUuid), eq(existingUuid), any(PatchedBaseAssessmentRequest.class));
            verify(riskAssessmentApi, never()).createRiskAssessment(any(), any());
        }

        @Test
        @DisplayName("should use first assessment and log warning when multiple exist")
        void multipleExistingAssessments_usesFirstOne() {
            String clientUuid = UUID.randomUUID().toString();
            UUID firstUuid = UUID.randomUUID();

            Assessment first = new Assessment(firstUuid, null, null, null, null, null, null, null);
            Assessment second = new Assessment(UUID.randomUUID(), null, null, null, null, null, null, null);
            PaginatedAssessmentList page = new PaginatedAssessmentList().results(List.of(first, second));
            OASBaseAssessment patched = new OASBaseAssessment(firstUuid, null, null, null, null, null, null, null);

            when(riskAssessmentApi.getRiskAssessments(eq(clientUuid), any(), any(), any()))
                .thenReturn(Mono.just(page));
            when(riskAssessmentApi.patchRiskAssessment(eq(clientUuid), eq(firstUuid), any()))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertRiskAssessments(clientUuid, List.of(new BaseAssessmentRequest())))
                .assertNext(result -> assertThat(result.get(0).getUuid()).isEqualTo(firstUuid))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertRiskAssessments – multiple assessments
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskAssessments – multiple assessments")
    class MultipleAssessmentsTests {

        @Test
        @DisplayName("should upsert all assessments and return collected results")
        void multipleAssessments_upsertAll() {
            String clientUuid = UUID.randomUUID().toString();
            PaginatedAssessmentList emptyPage = new PaginatedAssessmentList().results(List.of());
            OASBaseAssessment created = new OASBaseAssessment(UUID.randomUUID(), null, null, null, null, null, null, null);

            when(riskAssessmentApi.getRiskAssessments(eq(clientUuid), any(), any(), any()))
                .thenReturn(Mono.just(emptyPage));
            when(riskAssessmentApi.createRiskAssessment(eq(clientUuid), any()))
                .thenReturn(Mono.just(created));

            List<BaseAssessmentRequest> requests = List.of(
                new BaseAssessmentRequest(),
                new BaseAssessmentRequest()
            );

            StepVerifier.create(service.upsertRiskAssessments(clientUuid, requests))
                .assertNext(result -> assertThat(result).hasSize(2))
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertRiskAssessments – error handling
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskAssessments – error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should propagate WebClientResponseException from getRiskAssessments")
        void listError_propagatesException() {
            String clientUuid = UUID.randomUUID().toString();
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

            when(riskAssessmentApi.getRiskAssessments(eq(clientUuid), any(), any(), any()))
                .thenReturn(Mono.error(ex));

            StepVerifier.create(service.upsertRiskAssessments(clientUuid, List.of(new BaseAssessmentRequest())))
                .expectError(WebClientResponseException.class)
                .verify();
        }

        @Test
        @DisplayName("should propagate WebClientResponseException from createRiskAssessment")
        void createError_propagatesException() {
            String clientUuid = UUID.randomUUID().toString();
            PaginatedAssessmentList emptyPage = new PaginatedAssessmentList().results(List.of());
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

            when(riskAssessmentApi.getRiskAssessments(eq(clientUuid), any(), any(), any()))
                .thenReturn(Mono.just(emptyPage));
            when(riskAssessmentApi.createRiskAssessment(eq(clientUuid), any()))
                .thenReturn(Mono.error(ex));

            StepVerifier.create(service.upsertRiskAssessments(clientUuid, List.of(new BaseAssessmentRequest())))
                .expectError(WebClientResponseException.class)
                .verify();
        }
    }
}

