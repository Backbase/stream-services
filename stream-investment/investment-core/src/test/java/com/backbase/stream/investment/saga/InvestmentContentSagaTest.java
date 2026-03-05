package com.backbase.stream.investment.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentContentData;
import com.backbase.stream.investment.InvestmentContentTask;
import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.MarketNewsEntry;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import com.backbase.stream.worker.model.StreamTask.State;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit test suite for {@link InvestmentContentSaga}.
 *
 * <p>This class verifies the complete orchestration logic of the saga, which drives
 * the investment content ingestion pipeline through the following stages:
 * <ol>
 *   <li>Upsert news tags</li>
 *   <li>Upsert news content</li>
 *   <li>Upsert document tags</li>
 *   <li>Upsert content documents</li>
 * </ol>
 *
 * <p>Test strategy:
 * <ul>
 *   <li>Each pipeline stage is tested in isolation via a dedicated {@code @Nested} class.</li>
 *   <li>Happy-path, empty-collection, null-field, and error scenarios are covered for every stage.</li>
 *   <li>{@code wireTrivialPipelineAfter*()} helpers stub downstream stages so that each
 *       nested class can focus solely on its own stage under test.</li>
 *   <li>Error recovery is verified via the saga's {@code onErrorResume} handler, which
 *       always emits the task with {@link State#FAILED} instead of propagating the error
 *       signal — therefore {@link StepVerifier#verifyComplete()} is always used, never
 *       {@code verifyError()}.</li>
 *   <li>All reactive assertions use Project Reactor's {@link StepVerifier}.</li>
 * </ul>
 *
 * <p>Mocked dependencies:
 * <ul>
 *   <li>{@link InvestmentRestNewsContentService} – news tag and news content upsert</li>
 *   <li>{@link InvestmentRestDocumentContentService} – document tag and document upsert</li>
 *   <li>{@link InvestmentIngestionConfigurationProperties} – feature flag</li>
 * </ul>
 */
class InvestmentContentSagaTest {

    @Mock
    private InvestmentRestNewsContentService investmentRestNewsContentService;

    @Mock
    private InvestmentRestDocumentContentService investmentRestDocumentContentService;

    @Mock
    private InvestmentIngestionConfigurationProperties configurationProperties;

    private InvestmentContentSaga saga;

    /**
     * Opens Mockito annotations and constructs the saga under test before each test.
     * Content ingestion is enabled by default; individual tests may override this stub.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configurationProperties.isContentEnabled()).thenReturn(true);
        saga = new InvestmentContentSaga(
            investmentRestNewsContentService,
            investmentRestDocumentContentService,
            configurationProperties
        );
    }

    // =========================================================================
    // contentEnabled flag
    // =========================================================================

    /**
     * Tests for the {@code contentEnabled} configuration flag.
     *
     * <p>When {@code false}, the saga must return the task immediately without
     * invoking any downstream service.
     */
    @Nested
    @DisplayName("contentEnabled flag")
    class ContentEnabledFlagTests {

        /**
         * Verifies that when {@code contentEnabled} is {@code false}, the saga skips
         * all pipeline stages and returns the task without any service calls.
         */
        @Test
        @DisplayName("should skip saga execution when contentEnabled is false")
        void contentDisabled_skipsSagaExecution() {
            when(configurationProperties.isContentEnabled()).thenReturn(false);
            InvestmentContentTask task = createMinimalTask("disabled-task");

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result).isSameAs(task))
                .verifyComplete();

            verify(investmentRestNewsContentService, never()).upsertTags(anyList());
            verify(investmentRestNewsContentService, never()).upsertContent(anyList());
            verify(investmentRestDocumentContentService, never()).upsertContentTags(anyList());
            verify(investmentRestDocumentContentService, never()).upsertDocuments(anyList());
        }

        /**
         * Verifies that when {@code contentEnabled} is {@code true} the saga proceeds
         * through the full pipeline and the task reaches {@link State#COMPLETED}.
         * Uses a fully populated task to meaningfully exercise all pipeline stages.
         */
        @Test
        @DisplayName("should execute saga when contentEnabled is true")
        void contentEnabled_executesSaga() {
            InvestmentContentTask task = createFullTask("enabled-task");
            stubAllServicesSuccess();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();
        }
    }

    // =========================================================================
    // Full pipeline happy path
    // =========================================================================

    /**
     * End-to-end happy-path test covering all four pipeline stages with populated data.
     * All services are stubbed to return successful responses. The task is expected
     * to reach {@link State#COMPLETED}.
     */
    @Test
    @DisplayName("should complete full pipeline successfully when all data is present")
    void executeTask_fullPipeline_success() {
        InvestmentContentTask task = createFullTask("full-task");
        stubAllServicesSuccess();

        StepVerifier.create(saga.executeTask(task))
            .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
            .verifyComplete();
    }

    // =========================================================================
    // upsertNewsTags
    // =========================================================================

    /**
     * Tests for the {@code upsertNewsTags} stage of the content saga pipeline.
     *
     * <p>News tags are the first stage. The service must be called with the tag list
     * from the task data, and downstream stages are stubbed trivially so that the
     * task can reach a terminal state.
     */
    @Nested
    @DisplayName("upsertNewsTags")
    class UpsertNewsTagsTests {

        /**
         * Verifies that when the news-tag list is non-empty the tag service is called
         * and the task is marked {@link State#COMPLETED}.
         * Also verifies that a task history entry (info) is recorded for the stage.
         */
        @Test
        @DisplayName("should upsert news tags and mark task COMPLETED")
        void upsertNewsTags_success() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(List.of(buildContentTag("NEWS_TAG", "NewsValue")))
                .marketNews(Collections.emptyList())
                .documentTags(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("news-tags-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterNewsTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> {
                    assertThat(result.getState()).isEqualTo(State.COMPLETED);
                    assertThat(result.getHistory()).isNotEmpty();
                })
                .verifyComplete();

            verify(investmentRestNewsContentService).upsertTags(anyList());
        }

        /**
         * Verifies that an empty news-tag list is still forwarded to the service
         * (no early-exit in the implementation) and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should complete successfully when news tag list is empty")
        void upsertNewsTags_emptyList_completesSuccessfully() {
            InvestmentContentTask task = createMinimalTask("empty-news-tags-task");

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterNewsTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestNewsContentService).upsertTags(Collections.emptyList());
        }

        /**
         * Verifies that task state transitions through IN_PROGRESS before COMPLETED.
         */
        @Test
        @DisplayName("should transition task state through IN_PROGRESS then COMPLETED")
        void upsertNewsTags_stateTransition_inProgressThenCompleted() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(List.of(buildContentTag("NEWS_TAG", "NewsValue")))
                .marketNews(Collections.emptyList())
                .documentTags(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("news-tags-state-task", data);
            AtomicReference<State> stateAtServiceCall = new AtomicReference<>();

            when(investmentRestNewsContentService.upsertTags(anyList()))
                .thenAnswer(invocation -> {
                    stateAtServiceCall.set(task.getState());
                    return Mono.empty();
                });
            wireTrivialPipelineAfterNewsTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(stateAtServiceCall.get()).isEqualTo(State.IN_PROGRESS);
        }

        /**
         * Verifies that a news-tag upsert error causes the task to be marked
         * {@link State#FAILED} without propagating the error signal.
         */
        @Test
        @DisplayName("should mark task FAILED when news tag upsert throws an error")
        void upsertNewsTags_error_marksTaskFailed() {
            InvestmentContentTask task = createMinimalTask("news-tags-error-task");

            when(investmentRestNewsContentService.upsertTags(anyList()))
                .thenReturn(Mono.error(new RuntimeException("news tag upsert failed")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that null marketNewsTags in task data falls back to empty list
         * via {@code Objects.requireNonNullElse}, and the service is called with an empty list.
         */
        @Test
        @DisplayName("should handle null news tags field gracefully using empty list fallback")
        void upsertNewsTags_nullField_fallsBackToEmptyList() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(null)
                .marketNews(Collections.emptyList())
                .documentTags(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("null-news-tags-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterNewsTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestNewsContentService).upsertTags(Collections.emptyList());
        }
    }

    // =========================================================================
    // upsertNewsContent
    // =========================================================================

    /**
     * Tests for the {@code upsertNewsContent} stage of the content saga pipeline.
     *
     * <p>News content follows news tags. Each entry is forwarded to
     * {@link InvestmentRestNewsContentService#upsertContent}.
     */
    @Nested
    @DisplayName("upsertNewsContent")
    class UpsertNewsContentTests {

        /**
         * Verifies that when the news content list is non-empty the content service is
         * called and the task is marked {@link State#COMPLETED}.
         * Also verifies that a task history entry (info) is recorded for the stage.
         */
        @Test
        @DisplayName("should upsert news content and mark task COMPLETED")
        void upsertNewsContent_success() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(List.of(buildMarketNewsEntry("Market Update")))
                .documentTags(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("news-content-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterNewsContent();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> {
                    assertThat(result.getState()).isEqualTo(State.COMPLETED);
                    assertThat(result.getHistory()).isNotEmpty();
                })
                .verifyComplete();

            verify(investmentRestNewsContentService).upsertContent(anyList());
        }

        /**
         * Verifies that an empty news content list is still forwarded to the service
         * (no early-exit in the implementation) and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should complete successfully when news content list is empty")
        void upsertNewsContent_emptyList_completesSuccessfully() {
            InvestmentContentTask task = createMinimalTask("empty-news-content-task");

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterNewsContent();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestNewsContentService).upsertContent(Collections.emptyList());
        }

        /**
         * Verifies that task state transitions through IN_PROGRESS before COMPLETED.
         */
        @Test
        @DisplayName("should transition task state through IN_PROGRESS then COMPLETED")
        void upsertNewsContent_stateTransition_inProgressThenCompleted() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(List.of(buildMarketNewsEntry("Market Update")))
                .documentTags(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("news-content-state-task", data);
            AtomicReference<State> stateAtServiceCall = new AtomicReference<>();

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList()))
                .thenAnswer(invocation -> {
                    stateAtServiceCall.set(task.getState());
                    return Mono.empty();
                });
            wireTrivialPipelineAfterNewsContent();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(stateAtServiceCall.get()).isEqualTo(State.IN_PROGRESS);
        }

        /**
         * Verifies that a news content upsert error causes the task to be marked
         * {@link State#FAILED} without propagating the error signal.
         */
        @Test
        @DisplayName("should mark task FAILED when news content upsert throws an error")
        void upsertNewsContent_error_marksTaskFailed() {
            InvestmentContentTask task = createMinimalTask("news-content-error-task");

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList()))
                .thenReturn(Mono.error(new RuntimeException("news content upsert failed")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that null marketNews in task data falls back to empty list
         * via {@code Objects.requireNonNullElse}, and the service is called with an empty list.
         */
        @Test
        @DisplayName("should handle null news content field gracefully using empty list fallback")
        void upsertNewsContent_nullField_fallsBackToEmptyList() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(null)
                .documentTags(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("null-news-content-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterNewsContent();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestNewsContentService).upsertContent(Collections.emptyList());
        }
    }

    // =========================================================================
    // upsertDocumentTags
    // =========================================================================

    /**
     * Tests for the {@code upsertDocumentTags} stage of the content saga pipeline.
     *
     * <p>Document tags follow news content. Each entry is forwarded to
     * {@link InvestmentRestDocumentContentService#upsertContentTags}.
     */
    @Nested
    @DisplayName("upsertDocumentTags")
    class UpsertDocumentTagsTests {

        /**
         * Verifies that when the document-tag list is non-empty the document tag service
         * is called and the task is marked {@link State#COMPLETED}.
         * Also verifies that a task history entry (info) is recorded for the stage.
         */
        @Test
        @DisplayName("should upsert document tags and mark task COMPLETED")
        void upsertDocumentTags_success() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(Collections.emptyList())
                .documentTags(List.of(buildContentTag("DOC_TAG", "DocValue")))
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("doc-tags-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterDocumentTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> {
                    assertThat(result.getState()).isEqualTo(State.COMPLETED);
                    assertThat(result.getHistory()).isNotEmpty();
                })
                .verifyComplete();

            verify(investmentRestDocumentContentService).upsertContentTags(anyList());
        }

        /**
         * Verifies that an empty document-tag list is still forwarded to the service
         * (no early-exit in the implementation) and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should complete successfully when document tag list is empty")
        void upsertDocumentTags_emptyList_completesSuccessfully() {
            InvestmentContentTask task = createMinimalTask("empty-doc-tags-task");

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterDocumentTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestDocumentContentService).upsertContentTags(Collections.emptyList());
        }

        /**
         * Verifies that task state transitions through IN_PROGRESS before COMPLETED.
         */
        @Test
        @DisplayName("should transition task state through IN_PROGRESS then COMPLETED")
        void upsertDocumentTags_stateTransition_inProgressThenCompleted() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(Collections.emptyList())
                .documentTags(List.of(buildContentTag("DOC_TAG", "DocValue")))
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("doc-tags-state-task", data);
            AtomicReference<State> stateAtServiceCall = new AtomicReference<>();

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList()))
                .thenAnswer(invocation -> {
                    stateAtServiceCall.set(task.getState());
                    return Mono.empty();
                });
            wireTrivialPipelineAfterDocumentTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(stateAtServiceCall.get()).isEqualTo(State.IN_PROGRESS);
        }

        /**
         * Verifies that a document-tag upsert error causes the task to be marked
         * {@link State#FAILED} without propagating the error signal.
         */
        @Test
        @DisplayName("should mark task FAILED when document tag upsert throws an error")
        void upsertDocumentTags_error_marksTaskFailed() {
            InvestmentContentTask task = createMinimalTask("doc-tags-error-task");

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList()))
                .thenReturn(Mono.error(new RuntimeException("document tag upsert failed")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that null documentTags in task data falls back to empty list
         * via {@code Objects.requireNonNullElse}, and the service is called with an empty list.
         */
        @Test
        @DisplayName("should handle null document tags field gracefully using empty list fallback")
        void upsertDocumentTags_nullField_fallsBackToEmptyList() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(Collections.emptyList())
                .documentTags(null)
                .documents(Collections.emptyList())
                .build();
            InvestmentContentTask task = new InvestmentContentTask("null-doc-tags-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
            wireTrivialPipelineAfterDocumentTags();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestDocumentContentService).upsertContentTags(Collections.emptyList());
        }
    }

    // =========================================================================
    // upsertContentDocuments
    // =========================================================================

    /**
     * Tests for the {@code upsertContentDocuments} stage of the content saga pipeline.
     *
     * <p>This is the final stage. Document entries are forwarded to
     * {@link InvestmentRestDocumentContentService#upsertDocuments}.
     */
    @Nested
    @DisplayName("upsertContentDocuments")
    class UpsertContentDocumentsTests {

        /**
         * Verifies that when the document list is non-empty the document service is called
         * and the task is marked {@link State#COMPLETED}.
         * Also verifies that a task history entry (info) is recorded for the stage.
         */
        @Test
        @DisplayName("should upsert content documents and mark task COMPLETED")
        void upsertContentDocuments_success() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(Collections.emptyList())
                .documentTags(Collections.emptyList())
                .documents(List.of(buildContentDocumentEntry("Annual Report")))
                .build();
            InvestmentContentTask task = new InvestmentContentTask("docs-task", data);
            stubAllServicesSuccess();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> {
                    assertThat(result.getState()).isEqualTo(State.COMPLETED);
                    assertThat(result.getHistory()).isNotEmpty();
                })
                .verifyComplete();

            verify(investmentRestDocumentContentService).upsertDocuments(anyList());
        }

        /**
         * Verifies that an empty document list is still forwarded to the service
         * (no early-exit in the implementation) and the task is marked {@link State#COMPLETED}.
         */
        @Test
        @DisplayName("should complete successfully when document list is empty")
        void upsertContentDocuments_emptyList_completesSuccessfully() {
            InvestmentContentTask task = createMinimalTask("empty-docs-task");
            stubAllServicesSuccess();

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestDocumentContentService).upsertDocuments(Collections.emptyList());
        }

        /**
         * Verifies that task state transitions through IN_PROGRESS before COMPLETED.
         */
        @Test
        @DisplayName("should transition task state through IN_PROGRESS then COMPLETED")
        void upsertContentDocuments_stateTransition_inProgressThenCompleted() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(Collections.emptyList())
                .documentTags(Collections.emptyList())
                .documents(List.of(buildContentDocumentEntry("Annual Report")))
                .build();
            InvestmentContentTask task = new InvestmentContentTask("docs-state-task", data);
            AtomicReference<State> stateAtServiceCall = new AtomicReference<>();

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertDocuments(anyList()))
                .thenAnswer(invocation -> {
                    stateAtServiceCall.set(task.getState());
                    return Mono.empty();
                });

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            assertThat(stateAtServiceCall.get()).isEqualTo(State.IN_PROGRESS);
        }

        /**
         * Verifies that a document upsert error causes the task to be marked
         * {@link State#FAILED} without propagating the error signal.
         */
        @Test
        @DisplayName("should mark task FAILED when document upsert throws an error")
        void upsertContentDocuments_error_marksTaskFailed() {
            InvestmentContentTask task = createMinimalTask("docs-error-task");

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertDocuments(anyList()))
                .thenReturn(Mono.error(new RuntimeException("document upsert failed")));

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.FAILED))
                .verifyComplete();
        }

        /**
         * Verifies that null documents in task data falls back to empty list
         * via {@code Objects.requireNonNullElse}, and the service is called with an empty list.
         */
        @Test
        @DisplayName("should handle null documents field gracefully using empty list fallback")
        void upsertContentDocuments_nullField_fallsBackToEmptyList() {
            InvestmentContentData data = InvestmentContentData.builder()
                .marketNewsTags(Collections.emptyList())
                .marketNews(Collections.emptyList())
                .documentTags(Collections.emptyList())
                .documents(null)
                .build();
            InvestmentContentTask task = new InvestmentContentTask("null-docs-task", data);

            when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
            when(investmentRestDocumentContentService.upsertDocuments(anyList())).thenReturn(Mono.empty());

            StepVerifier.create(saga.executeTask(task))
                .assertNext(result -> assertThat(result.getState()).isEqualTo(State.COMPLETED))
                .verifyComplete();

            verify(investmentRestDocumentContentService).upsertDocuments(Collections.emptyList());
        }
    }

    // =========================================================================
    // rollBack
    // =========================================================================

    /**
     * Tests for the {@link InvestmentContentSaga#rollBack} operation.
     *
     * <p>Rollback is intentionally not implemented for the content saga because all
     * operations are idempotent. The method must return an empty {@link Mono}.
     */
    @Nested
    @DisplayName("rollBack")
    class RollBackTests {

        /**
         * Verifies that {@code rollBack} completes empty without invoking any service.
         */
        @Test
        @DisplayName("should return empty Mono without calling any service")
        void rollBack_returnsEmptyMono() {
            InvestmentContentTask task = createMinimalTask("rollback-task");

            StepVerifier.create(saga.rollBack(task))
                .verifyComplete();

            verify(investmentRestNewsContentService, never()).upsertTags(anyList());
            verify(investmentRestNewsContentService, never()).upsertContent(anyList());
            verify(investmentRestDocumentContentService, never()).upsertContentTags(anyList());
            verify(investmentRestDocumentContentService, never()).upsertDocuments(anyList());
        }
    }

    // =========================================================================
    // Helper / Builder Methods
    // =========================================================================

    private InvestmentContentTask createMinimalTask(String taskId) {
        InvestmentContentData data = InvestmentContentData.builder()
            .marketNewsTags(Collections.emptyList())
            .marketNews(Collections.emptyList())
            .documentTags(Collections.emptyList())
            .documents(Collections.emptyList())
            .build();
        return new InvestmentContentTask(taskId, data);
    }

    private InvestmentContentTask createFullTask(String taskId) {
        InvestmentContentData data = InvestmentContentData.builder()
            .marketNewsTags(List.of(buildContentTag("NEWS_TAG", "NewsValue")))
            .marketNews(List.of(buildMarketNewsEntry("Market Update")))
            .documentTags(List.of(buildContentTag("DOC_TAG", "DocValue")))
            .documents(List.of(buildContentDocumentEntry("Annual Report")))
            .build();
        return new InvestmentContentTask(taskId, data);
    }

    private void stubAllServicesSuccess() {
        when(investmentRestNewsContentService.upsertTags(anyList())).thenReturn(Mono.empty());
        when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
        when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
        when(investmentRestDocumentContentService.upsertDocuments(anyList())).thenReturn(Mono.empty());
    }

    private void wireTrivialPipelineAfterNewsTags() {
        when(investmentRestNewsContentService.upsertContent(anyList())).thenReturn(Mono.empty());
        when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
        when(investmentRestDocumentContentService.upsertDocuments(anyList())).thenReturn(Mono.empty());
    }

    private void wireTrivialPipelineAfterNewsContent() {
        when(investmentRestDocumentContentService.upsertContentTags(anyList())).thenReturn(Mono.empty());
        when(investmentRestDocumentContentService.upsertDocuments(anyList())).thenReturn(Mono.empty());
    }

    private void wireTrivialPipelineAfterDocumentTags() {
        when(investmentRestDocumentContentService.upsertDocuments(anyList())).thenReturn(Mono.empty());
    }

    private ContentTag buildContentTag(String code, String value) {
        return new ContentTag(code, value);
    }

    private MarketNewsEntry buildMarketNewsEntry(String title) {
        MarketNewsEntry entry = new MarketNewsEntry();
        entry.setTitle(title);
        return entry;
    }

    private ContentDocumentEntry buildContentDocumentEntry(String name) {
        ContentDocumentEntry entry = new ContentDocumentEntry();
        entry.setName(name);
        return entry;
    }
}
