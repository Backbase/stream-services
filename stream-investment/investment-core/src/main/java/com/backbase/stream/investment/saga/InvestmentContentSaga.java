package com.backbase.stream.investment.saga;

import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentContentTask;
import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.model.MarketNewsEntry;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask.State;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Saga orchestrating the complete investment content ingestion workflow.
 *
 * <p>This saga implements a multi-step process for ingesting investment content data:
 * <ol>
 *   <li>Upsert news tags - Creates or updates market news tags</li>
 *   <li>Upsert news content - Creates or updates market news entries</li>
 *   <li>Upsert document tags - Creates or updates content document tags</li>
 *   <li>Upsert content documents - Creates or updates content document entries</li>
 * </ol>
 *
 * <p>The saga uses idempotent operations to ensure safe re-execution and writes progress
 * to the {@link com.backbase.stream.worker.model.StreamTask} history for observability.
 * Each step builds upon the previous step's results, creating a complete content setup.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All operations are idempotent (safe to retry)</li>
 *   <li>Progress is tracked via StreamTask state and history</li>
 *   <li>Failures are logged with complete context for debugging</li>
 *   <li>All reactive operations include proper success and error handlers</li>
 *   <li>Content ingestion can be disabled via {@link com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties}</li>
 * </ul>
 *
 * @see InvestmentRestNewsContentService
 * @see InvestmentRestDocumentContentService
 * @see StreamTaskExecutor
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentContentSaga implements StreamTaskExecutor<InvestmentContentTask> {

    public static final String INVESTMENT = "investment-content";
    public static final String OP_UPSERT = "upsert";
    public static final String RESULT_UPSERTED = "upserted";
    public static final String RESULT_FAILED = "failed";

    private static final String PROCESSING_PREFIX = "Processing ";

    private final InvestmentRestNewsContentService investmentRestNewsContentService;
    private final InvestmentRestDocumentContentService investmentRestDocumentContentService;
    private final InvestmentIngestionConfigurationProperties coreConfigurationProperties;

    @Override
    public Mono<InvestmentContentTask> executeTask(InvestmentContentTask streamTask) {
        if (!coreConfigurationProperties.isContentEnabled()) {
            log.warn("Skip investment content saga execution: taskId={}, taskName={}",
                streamTask.getId(), streamTask.getName());
            return Mono.just(streamTask);
        }
        log.info("Starting investment content saga execution: taskId={}, taskName={}",
            streamTask.getId(), streamTask.getName());
        return upsertNewsTags(streamTask)
            .flatMap(this::upsertNewsContent)
            .flatMap(this::upsertDocumentTags)
            .flatMap(this::upsertContentDocuments)
            .doOnSuccess(completedTask -> log.info(
                "Successfully completed investment content saga: taskId={}, taskName={}, state={}",
                completedTask.getId(), completedTask.getName(), completedTask.getState()))
            .doOnError(throwable -> {
                log.error("Failed to execute investment content saga: taskId={}, taskName={}",
                    streamTask.getId(), streamTask.getName(), throwable);
                streamTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED,
                    streamTask.getName(), streamTask.getId(),
                    "Investment content saga failed: " + throwable.getMessage());
                streamTask.setState(State.FAILED);
            })
            .onErrorResume(throwable -> Mono.just(streamTask));
    }

    private Mono<InvestmentContentTask> upsertNewsContent(InvestmentContentTask investmentContentTask) {
        List<MarketNewsEntry> marketNews = Objects.requireNonNullElse(investmentContentTask.getData().getMarketNews(), List.of());
        investmentContentTask.info(INVESTMENT, OP_UPSERT, null, investmentContentTask.getName(),
            investmentContentTask.getId(),
            PROCESSING_PREFIX + marketNews.size() + " investment news content");
        investmentContentTask.setState(State.IN_PROGRESS);
        return investmentRestNewsContentService
            .upsertContent(marketNews)
            .doOnSuccess(v -> {
                investmentContentTask.info(INVESTMENT, OP_UPSERT, RESULT_UPSERTED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    RESULT_UPSERTED + " " + marketNews.size() + " Investment News Content");
                investmentContentTask.setState(State.COMPLETED);
            })
            .doOnError(throwable ->
                investmentContentTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    "Failed to upsert investment news content: " + throwable.getMessage()))
            .thenReturn(investmentContentTask);
    }

    private Mono<InvestmentContentTask> upsertNewsTags(InvestmentContentTask investmentContentTask) {
        List<ContentTag> newsTags = Objects.requireNonNullElse(investmentContentTask.getData().getMarketNewsTags(), List.of());
        investmentContentTask.info(INVESTMENT, OP_UPSERT, null, investmentContentTask.getName(),
            investmentContentTask.getId(),
            PROCESSING_PREFIX + newsTags.size() + " investment news tags");
        investmentContentTask.setState(State.IN_PROGRESS);
        return investmentRestNewsContentService
            .upsertTags(newsTags)
            .doOnSuccess(v -> {
                investmentContentTask.info(INVESTMENT, OP_UPSERT, RESULT_UPSERTED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    RESULT_UPSERTED + " " + newsTags.size() + " Investment News Tags");
                investmentContentTask.setState(State.COMPLETED);
            })
            .doOnError(throwable ->
                investmentContentTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    "Failed to upsert investment news tags: " + throwable.getMessage()))
            .thenReturn(investmentContentTask);
    }

    private Mono<InvestmentContentTask> upsertDocumentTags(InvestmentContentTask investmentContentTask) {
        List<ContentTag> documentTags = Objects.requireNonNullElse(investmentContentTask.getData().getDocumentTags(), List.of());
        investmentContentTask.info(INVESTMENT, OP_UPSERT, null, investmentContentTask.getName(),
            investmentContentTask.getId(),
            PROCESSING_PREFIX + documentTags.size() + " investment document tags");
        investmentContentTask.setState(State.IN_PROGRESS);
        return investmentRestDocumentContentService
            .upsertContentTags(documentTags)
            .doOnSuccess(v -> {
                investmentContentTask.info(INVESTMENT, OP_UPSERT, RESULT_UPSERTED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    RESULT_UPSERTED + " " + documentTags.size() + " Investment Document Tags");
                investmentContentTask.setState(State.COMPLETED);
            })
            .doOnError(throwable ->
                investmentContentTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    "Failed to upsert investment document tags: " + throwable.getMessage()))
            .thenReturn(investmentContentTask);
    }

    private Mono<InvestmentContentTask> upsertContentDocuments(InvestmentContentTask investmentContentTask) {
        List<ContentDocumentEntry> documents =
            Objects.requireNonNullElse(investmentContentTask.getData().getDocuments(), List.of());
        investmentContentTask.info(INVESTMENT, OP_UPSERT, null, investmentContentTask.getName(),
            investmentContentTask.getId(),
            PROCESSING_PREFIX + documents.size() + " investment content documents");
        investmentContentTask.setState(State.IN_PROGRESS);
        return investmentRestDocumentContentService
            .upsertDocuments(documents)
            .doOnSuccess(v -> {
                investmentContentTask.info(INVESTMENT, OP_UPSERT, RESULT_UPSERTED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    RESULT_UPSERTED + " " + documents.size() + " Investment Content Documents");
                investmentContentTask.setState(State.COMPLETED);
            })
            .doOnError(throwable ->
                investmentContentTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED, investmentContentTask.getName(),
                    investmentContentTask.getId(),
                    "Failed to upsert investment content documents: " + throwable.getMessage()))
            .thenReturn(investmentContentTask);
    }

    /**
     * Rollback is not implemented for investment saga.
     *
     * <p>Investment operations are idempotent and designed to be retried safely.
     * Manual cleanup should be performed if necessary through the Investment Service API.
     *
     * @param streamTask the task to rollback
     * @return null - rollback not implemented
     */
    @Override
    public Mono<InvestmentContentTask> rollBack(InvestmentContentTask streamTask) {
        log.warn("Rollback requested for investment saga but not implemented: taskId={}, taskName={}",
            streamTask.getId(), streamTask.getName());
        return Mono.empty();
    }

}
