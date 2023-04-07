package com.backbase.stream.portfolio;

import com.backbase.stream.portfolio.exceptions.PortfolioBundleException;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.service.ReactiveStreamHandler;
import com.backbase.stream.worker.StreamTaskExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;

import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class PortfolioSaga implements StreamTaskExecutor<PortfolioTask> {

    private static final String REGION_ENTITY = "REGION_ENTITY";
    private static final String PORTFOLIO_ENTITY = "PORTFOLIO_ENTITY";
    private static final String AGGREGATE_PORTFOLIO_ENTITY = "PORTFOLIO_ENTITY";
    private static final String INSTRUMENT_ENTITY = "INSTRUMENT_ENTITY";
    private static final String POSITION_ENTITY = "POSITION_ENTITY";
    private static final String ASSET_CLASS_ENTITY = "ASSET_CLASS_ENTITY";
    private static final String UPSERT_REGIONS = "upsert-regions";
    private static final String UPSERT_PORTFOLIOS = "upsert-portfolios";
    private static final String INSERT_AGGREGATE_PORTFOLIOS = "upsert-aggregate-portfolios";
    private static final String UPSERT_INSTRUMENTS = "upsert-instruments";
    private static final String UPSERT_POSITIONS = "upsert-positions";
    private static final String UPSERT_ASSET_CLASSES = "upsert-asset-classes";
    private static final String UPSERT = "upsert";

    private final PortfolioIntegrationService portfolioIntegrationService;
    private final InstrumentIntegrationService instrumentIntegrationService;

    @Override
    public Mono<PortfolioTask> executeTask(PortfolioTask streamTask) {
        return upsertRegions(streamTask)
                .flatMap(this::upsertAssetClasses)
                .flatMap(this::upsertInstruments)
                .flatMap(this::upsertPortfolios)
                .flatMap(this::upsertPositions)
                .flatMap(this::upsertAggregatePortfolios);
    }

    @Override
    public Mono<PortfolioTask> rollBack(PortfolioTask streamTask) {
        // GET CREATED AND EVENTS AND CALL DELETE ENDPOINTS IN REVERSE
        return Mono.just(streamTask);
    }

    @ContinueSpan(log = UPSERT_POSITIONS)
    private Mono<PortfolioTask> upsertPositions(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(INSTRUMENT_ENTITY, UPSERT, null, null, null, "Upsert positions");
        return ReactiveStreamHandler.getFluxStream(task.getData().getPositions())
                .flatMap(portfolioIntegrationService::upsertPosition)
                .onErrorResume(
                        PortfolioBundleException.class,
                        ReactiveStreamHandler.error(task, POSITION_ENTITY, UPSERT_POSITIONS))
                .collectList()
                .map(o -> task);
    }

    @ContinueSpan(log = UPSERT_INSTRUMENTS)
    private Mono<PortfolioTask> upsertInstruments(
            @SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(INSTRUMENT_ENTITY, UPSERT, null, null, null, "Upsert instruments");
        return ReactiveStreamHandler.getFluxStream(task.getData().getInstruments())
                .flatMap(instrumentIntegrationService::upsertInstrument)
                .onErrorResume(
                        PortfolioBundleException.class,
                        ReactiveStreamHandler.error(task, INSTRUMENT_ENTITY, UPSERT_INSTRUMENTS))
                .collectList()
                .map(o -> task);
    }

    @ContinueSpan(log = INSERT_AGGREGATE_PORTFOLIOS)
    private Mono<PortfolioTask> upsertAggregatePortfolios(
            @SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(
                AGGREGATE_PORTFOLIO_ENTITY,
                UPSERT,
                null,
                null,
                null,
                "Upsert aggregate portfolios");
        return ReactiveStreamHandler.getFluxStream(task.getData().getAggregatePortfolios())
                .flatMap(portfolioIntegrationService::createAggregatePortfolio)
                .onErrorResume(
                        PortfolioBundleException.class,
                        ReactiveStreamHandler.error(
                                task, AGGREGATE_PORTFOLIO_ENTITY, INSERT_AGGREGATE_PORTFOLIOS))
                .collectList()
                .map(o -> task);
    }

    @ContinueSpan(log = UPSERT_ASSET_CLASSES)
    private Mono<PortfolioTask> upsertAssetClasses(
            @SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(ASSET_CLASS_ENTITY, UPSERT, null, null, null, "Upsert asset classes");
        return instrumentIntegrationService
                .upsertAssetClass(task.getData().getAssetClasses())
                .map(o -> task);
    }

    @ContinueSpan(log = UPSERT_PORTFOLIOS)
    private Mono<PortfolioTask> upsertPortfolios(
            @SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(PORTFOLIO_ENTITY, UPSERT, null, null, null, "Upsert portfolios");
        return ReactiveStreamHandler.getFluxStream(task.getData().getPortfolios())
                .flatMap(portfolioIntegrationService::upsertPortfolio)
                .onErrorResume(
                        PortfolioBundleException.class,
                        ReactiveStreamHandler.error(task, PORTFOLIO_ENTITY, UPSERT_PORTFOLIOS))
                .collectList()
                .map(o -> task);
    }

    @ContinueSpan(log = UPSERT_REGIONS)
    private Mono<PortfolioTask> upsertRegions(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(REGION_ENTITY, UPSERT, null, null, null, "Upsert Regions");
        log.info("upsert region");
        return instrumentIntegrationService
                .upsertRegions(task.getData().getRegions())
                .map(o -> task);
    }
}
