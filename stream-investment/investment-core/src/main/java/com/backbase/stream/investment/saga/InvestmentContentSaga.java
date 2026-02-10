package com.backbase.stream.investment.saga;

import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentContentTask;
import com.backbase.stream.investment.ModelAsset;
import com.backbase.stream.investment.model.ContentDocumentEntry;
import com.backbase.stream.investment.model.ContentTag;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestDocumentContentService;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestNewsContentService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.StreamTask.State;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

/**
 * Saga orchestrating the complete investment client ingestion workflow.
 *
 * <p>This saga implements a multi-step process for ingesting investment data:
 * <ol>
 *   <li>Upsert investment clients - Creates or updates client records</li>
 *   <li>Upsert investment products - Creates or updates portfolio products</li>
 *   <li>Upsert investment portfolios - Creates or updates portfolios with client associations</li>
 * </ol>
 *
 * <p>The saga uses idempotent operations to ensure safe re-execution and writes progress
 * to the {@link StreamTask} history for observability. Each step builds upon the previous
 * step's results, creating a complete investment setup.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All operations are idempotent (safe to retry)</li>
 *   <li>Progress is tracked via StreamTask state and history</li>
 *   <li>Failures are logged with complete context for debugging</li>
 *   <li>All reactive operations include proper success and error handlers</li>
 * </ul>
 *
 * @see InvestmentClientService
 * @see InvestmentPortfolioService
 * @see StreamTaskExecutor
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentContentSaga implements StreamTaskExecutor<InvestmentContentTask> {

    public static final String INVESTMENT = "investment-content";
    public static final String OP_UPSERT = "upsert";
    public static final String RESULT_FAILED = "failed";

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
        log.info("Starting investment saga execution: taskId={}, taskName={}",
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
        return investmentRestNewsContentService
            .upsertContent(Objects.requireNonNullElse(investmentContentTask.getData().getMarketNews(), List.of()))
            .thenReturn(investmentContentTask);
    }

    private Mono<InvestmentContentTask> upsertNewsTags(InvestmentContentTask investmentContentTask) {
        return investmentRestNewsContentService
            .upsertTags(Objects.requireNonNullElse(investmentContentTask.getData().getMarketNewsTags(), List.of()))
            .thenReturn(investmentContentTask);
    }

    private Mono<InvestmentContentTask> upsertDocumentTags(InvestmentContentTask investmentContentTask) {
        // TODO: remove
        List<ContentTag> selfTrading = List.of(
            new ContentTag("self-trading", "Self Trading"),
            new ContentTag("self-trading2", "Self Trading2")
        );
        return investmentRestDocumentContentService
            .upsertContentTags(Objects.requireNonNullElse(investmentContentTask.getData().getDocumentTags(), List.of()))
            .thenReturn(investmentContentTask);
    }

    private Mono<InvestmentContentTask> upsertContentDocuments(InvestmentContentTask investmentContentTask) {
        List<ContentDocumentEntry> documents = investmentContentTask.getData().getDocuments();

        // TODO: remove
        //        Map<String, Object> isin1 = Map.of("isin", "LU1681047236", "market", "XETR", "currency", "EUR");
        List<ModelAsset> isin2 = List.of(new ModelAsset("LU1681047236", "XETR", "EUR"));
        File file = new File(
            "/Users/r.kniazevych/work/backbase/BSJ/rnd-bootstrap-job/data/src/main/resources/non-modelbank/investment/content-documents/files/product_robo-advisor_en.pdf");
        Resource fileSystemResource = new FileSystemResource(file);
        List<ContentDocumentEntry> t1 = List.of(new ContentDocumentEntry(
            "Test Document",
            "A test document",
            List.of("self-trading"),
            isin2, Map.of("createdBy", "bootstrap-job"), "test", fileSystemResource)
            ,
            new ContentDocumentEntry(
                "Test Document2",
                "A test document",
                List.of("self-trading"),
                isin2, Map.of("createdBy", "bootstrap-job"), "test", fileSystemResource)
        );

        return investmentRestDocumentContentService
            .upsertDocuments(Objects.requireNonNullElse(documents, List.of()))
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

