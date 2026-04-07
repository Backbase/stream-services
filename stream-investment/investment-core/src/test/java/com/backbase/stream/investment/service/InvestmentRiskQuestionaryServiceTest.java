package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.RiskAssessmentApi;
import com.backbase.investment.api.service.v1.model.BaseRiskQuestion;
import com.backbase.investment.api.service.v1.model.OASBaseRiskChoice;
import com.backbase.investment.api.service.v1.model.OASRiskQuestion;
import com.backbase.investment.api.service.v1.model.PaginatedOASRiskQuestionList;
import com.backbase.investment.api.service.v1.model.PaginatedRiskChoiceList;
import com.backbase.investment.api.service.v1.model.RiskChoice;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.model.QuestionChoice;
import com.backbase.stream.investment.model.RiskQuestion;
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
 * Unit test suite for {@link InvestmentRiskQuestionaryService}.
 *
 * <p>Covers the upsert pattern for risk questions and their choices:
 * <ul>
 *   <li>Empty input → empty result, no API calls</li>
 *   <li>No existing question → create path</li>
 *   <li>Existing question → patch path</li>
 *   <li>Choices created/patched after questions are upserted</li>
 *   <li>WebClient errors on choices are recovered (soft error)</li>
 * </ul>
 */
class InvestmentRiskQuestionaryServiceTest {

    @Mock
    private RiskAssessmentApi riskAssessmentApi;

    private IngestConfigProperties ingestProperties;
    private InvestmentRiskQuestionaryService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        ingestProperties = new IngestConfigProperties();
        service = new InvestmentRiskQuestionaryService(riskAssessmentApi, ingestProperties);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // =========================================================================
    // upsertRiskQuestions – empty input
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskQuestions – empty input")
    class EmptyInputTests {

        @Test
        @DisplayName("should return empty list and skip API calls when question list is empty")
        void emptyList_returnsEmptyListAndSkipsApiCalls() {
            // Choices phase also needs listRiskChoices stub for empty list
            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.just(new PaginatedRiskChoiceList().results(List.of())));

            StepVerifier.create(service.upsertRiskQuestions(List.of()))
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();

            verify(riskAssessmentApi, never()).listRiskQuestions(any(), any());
        }

        @Test
        @DisplayName("should treat null question list as empty")
        void nullList_treatedAsEmpty() {
            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.just(new PaginatedRiskChoiceList().results(List.of())));

            StepVerifier.create(service.upsertRiskQuestions(null))
                .assertNext(result -> assertThat(result).isEmpty())
                .verifyComplete();
        }
    }

    // =========================================================================
    // upsertRiskQuestions – create path
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskQuestions – create path")
    class CreatePathTests {

        @Test
        @DisplayName("should create a new question when none exists with the given code")
        void noExistingQuestion_createsNew() {
            RiskQuestion question = buildQuestion("Q1", 1);

            PaginatedOASRiskQuestionList emptyPage = new PaginatedOASRiskQuestionList().results(List.of());
            PaginatedRiskChoiceList emptyChoicePage = new PaginatedRiskChoiceList().results(List.of());

            when(riskAssessmentApi.listRiskQuestions(any(), any()))
                .thenReturn(Mono.just(emptyPage));
            when(riskAssessmentApi.createRiskQuestion(any()))
                .thenReturn(Mono.just(new BaseRiskQuestion().code("Q1")));
            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.just(emptyChoicePage));
            when(riskAssessmentApi.createRiskChoice(any()))
                .thenReturn(Mono.just(new OASBaseRiskChoice().code("C1")));

            StepVerifier.create(service.upsertRiskQuestions(List.of(question)))
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();

            verify(riskAssessmentApi).createRiskQuestion(any());
            verify(riskAssessmentApi, never()).patchRiskQuestion(any(), any());
        }
    }

    // =========================================================================
    // upsertRiskQuestions – patch path
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskQuestions – patch path")
    class PatchPathTests {

        @Test
        @DisplayName("should patch the existing question when one is found with matching code")
        void existingQuestion_patchesExisting() {
            UUID existingUuid = UUID.randomUUID();
            RiskQuestion question = buildQuestion("Q1", 1);

            // Use @JsonCreator constructor: (uuid, created, updated)
            OASRiskQuestion existing = new OASRiskQuestion(existingUuid, null, null);
            existing.setCode("Q1");
            PaginatedOASRiskQuestionList page = new PaginatedOASRiskQuestionList().results(List.of(existing));
            PaginatedRiskChoiceList emptyChoicePage = new PaginatedRiskChoiceList().results(List.of());

            when(riskAssessmentApi.listRiskQuestions(any(), any()))
                .thenReturn(Mono.just(page));
            when(riskAssessmentApi.patchRiskQuestion(eq(existingUuid), any()))
                .thenReturn(Mono.just(new BaseRiskQuestion().code("Q1")));
            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.just(emptyChoicePage));
            when(riskAssessmentApi.createRiskChoice(any()))
                .thenReturn(Mono.just(new OASBaseRiskChoice().code("C1")));

            StepVerifier.create(service.upsertRiskQuestions(List.of(question)))
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();

            verify(riskAssessmentApi).patchRiskQuestion(eq(existingUuid), any());
            verify(riskAssessmentApi, never()).createRiskQuestion(any());
        }
    }

    // =========================================================================
    // upsertRiskQuestionsChoices – choice upsert
    // =========================================================================

    @Nested
    @DisplayName("upsertRiskQuestionsChoices – choice upsert")
    class ChoiceUpsertTests {

        @Test
        @DisplayName("should create choice when none exists for the question")
        void noExistingChoice_createsNew() {
            RiskQuestion question = buildQuestion("Q1", 1);
            PaginatedRiskChoiceList emptyPage = new PaginatedRiskChoiceList().results(List.of());

            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.just(emptyPage));
            when(riskAssessmentApi.createRiskChoice(any()))
                .thenReturn(Mono.just(new OASBaseRiskChoice().code("C1")));

            StepVerifier.create(service.upsertRiskQuestionsChoices(List.of(question)))
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();

            verify(riskAssessmentApi).createRiskChoice(any());
        }

        @Test
        @DisplayName("should patch choice when one exists for the question")
        void existingChoice_patchesExisting() {
            UUID choiceUuid = UUID.randomUUID();
            RiskQuestion question = buildQuestion("Q1", 1);

            BaseRiskQuestion questionRef = new BaseRiskQuestion().code("Q1");
            // Use @JsonCreator constructor: (uuid, created, updated)
            RiskChoice existing = new RiskChoice(choiceUuid, null, null)
                .code("C1")
                .question(questionRef);
            PaginatedRiskChoiceList page = new PaginatedRiskChoiceList().results(List.of(existing));

            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.just(page));
            when(riskAssessmentApi.patchRiskChoice(eq(choiceUuid), any()))
                .thenReturn(Mono.just(new OASBaseRiskChoice().code("C1")));

            StepVerifier.create(service.upsertRiskQuestionsChoices(List.of(question)))
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();

            verify(riskAssessmentApi).patchRiskChoice(eq(choiceUuid), any());
            verify(riskAssessmentApi, never()).createRiskChoice(any());
        }

        @Test
        @DisplayName("should recover and return input when WebClientResponseException occurs on choices")
        void choiceApiError_recoversAndReturnsInput() {
            RiskQuestion question = buildQuestion("Q1", 1);
            WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

            when(riskAssessmentApi.listRiskChoices(any(), any()))
                .thenReturn(Mono.error(ex));

            StepVerifier.create(service.upsertRiskQuestionsChoices(List.of(question)))
                .assertNext(result -> assertThat(result).hasSize(1))
                .verifyComplete();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private RiskQuestion buildQuestion(String code, int order) {
        RiskQuestion q = new RiskQuestion();
        q.setCode(code);
        q.setOrder(order);
        q.setDescription("Test question " + code);
        q.setScore(5.0);
        QuestionChoice choice = new QuestionChoice();
        choice.setCode("C1");
        choice.setOrder(1);
        choice.setDescription("Choice 1");
        choice.setScore(3.0);
        q.setChoices(List.of(choice));
        return q;
    }
}

