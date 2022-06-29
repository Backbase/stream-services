package com.backbase.stream.portfolio;

import com.backbase.stream.config.BackbaseStreamConfigurationProperties;
import com.backbase.stream.portfolio.exceptions.PortfolioBundleException;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import com.backbase.stream.portfolio.service.ReactiveStreamHandler;
import com.backbase.stream.webclient.DbsWebClientConfiguration;
import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@EnableConfigurationProperties(BackbaseStreamConfigurationProperties.class)
@Import(DbsWebClientConfiguration.class)
@Slf4j
@RequiredArgsConstructor
@Component
public class PortfolioSaga implements StreamTaskExecutor<PortfolioTask> {

    private static final String REGION_ENTITY = "REGION_ENTITY";
    private static final String PORTFOLIO_ENTITY = "PORTFOLIO_ENTITY";
    private static final String AGGREGATE_PORTFOLIO_ENTITY = "PORTFOLIO_ENTITY";
    private static final String INSTRUMENT_ENTITY = "INSTRUMENT_ENTITY";
    private static final String POSITION_ENTITY = "POSITION_ENTITY";
    private static final String ASSET_CLASS_ENTITY = "ASSET_CLASS_ENTITY";
    private static final String INSERT_REGIONS = "insert-regions";
    private static final String INSERT_PORTFOLIOS = "insert-portfolios";
    private static final String INSERT_AGGREGATE_PORTFOLIOS = "insert-aggregate-portfolios";
    private static final String INSERT_INSTRUMENTS = "insert-instruments";
    private static final String INSERT_POSITIONS = "insert-positions";
    private static final String INSERT_ASSET_CLASSES = "insert-asset-classes";
    private static final String INSERT = "insert";

    private final PortfolioIntegrationService portfolioIntegrationService;
    private final InstrumentIntegrationService instrumentIntegrationService;

    @Override
    public Mono<PortfolioTask> executeTask(PortfolioTask streamTask) {
        return insertRegions(streamTask)
            .flatMap(this::insertAssetClasses)
            .flatMap(this::insertInstruments)
            .flatMap(this::insertPortfolios)
            .flatMap(this::insertPositions)
            .flatMap(this::insertAggregatePortfolios);
    }

    @Override
    public Mono<PortfolioTask> rollBack(PortfolioTask streamTask) {
        // GET CREATED AND EVENTS AND CALL DELETE ENDPOINTS IN REVERSE
        return Mono.just(streamTask);
    }

    @ContinueSpan(log = INSERT_POSITIONS)
    private Mono<PortfolioTask> insertPositions(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(INSTRUMENT_ENTITY, INSERT, null, null, null, "Upsert instruments");
        return ReactiveStreamHandler.getFluxStream(task.getData().getPositions())
            .flatMap(portfolioIntegrationService::createPosition)
            .onErrorResume(PortfolioBundleException.class,
                ReactiveStreamHandler.error(task, POSITION_ENTITY, INSERT_POSITIONS))
            .collectList()
            .map(o -> task);
    }

    @ContinueSpan(log = INSERT_INSTRUMENTS)
    private Mono<PortfolioTask> insertInstruments(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(INSTRUMENT_ENTITY, INSERT, null, null, null, "Upsert instruments");
        return ReactiveStreamHandler.getFluxStream(task.getData().getInstruments())
            .flatMap(instrumentIntegrationService::createInstrument)
            .onErrorResume(PortfolioBundleException.class,
                ReactiveStreamHandler.error(task, INSTRUMENT_ENTITY, INSERT_INSTRUMENTS))
            .collectList()
            .map(o -> task);

    }

    @ContinueSpan(log = INSERT_AGGREGATE_PORTFOLIOS)
    private Mono<PortfolioTask> insertAggregatePortfolios(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(AGGREGATE_PORTFOLIO_ENTITY, INSERT, null, null, null, "Upsert aggregate portfolios");
        return ReactiveStreamHandler.getFluxStream(task.getData().getAggregatePortfolios())
            .flatMap(portfolioIntegrationService::createAggregatePortfolio)
            .onErrorResume(PortfolioBundleException.class,
                ReactiveStreamHandler.error(task, AGGREGATE_PORTFOLIO_ENTITY, INSERT_AGGREGATE_PORTFOLIOS))
            .collectList()
            .map(o -> task);

    }

    @ContinueSpan(log = INSERT_ASSET_CLASSES)
    private Mono<PortfolioTask> insertAssetClasses(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(ASSET_CLASS_ENTITY, INSERT, null, null, null, "Upsert asset classes");
        return ReactiveStreamHandler.getFluxStream(task.getData().getAssetClasses())
            .flatMap(instrumentIntegrationService::createAssetClass)
            .onErrorResume(PortfolioBundleException.class,
                ReactiveStreamHandler.error(task, ASSET_CLASS_ENTITY, INSERT_ASSET_CLASSES))
            .collectList()
            .map(o -> task);
    }

    @ContinueSpan(log = INSERT_PORTFOLIOS)
    private Mono<PortfolioTask> insertPortfolios(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(PORTFOLIO_ENTITY, INSERT, null, null, null, "Upsert portfolios");
        return ReactiveStreamHandler.getFluxStream(task.getData().getPortfolios())
            .flatMap(portfolioIntegrationService::createPortfolio)
            .onErrorResume(PortfolioBundleException.class,
                ReactiveStreamHandler.error(task, PORTFOLIO_ENTITY, INSERT_PORTFOLIOS))
            .collectList()
            .map(o -> task);
    }

    @ContinueSpan(log = INSERT_REGIONS)
    private Mono<PortfolioTask> insertRegions(@SpanTag(value = "streamTask") PortfolioTask task) {
        task.info(REGION_ENTITY, INSERT, null, null, null, "Upsert Regions");
        return ReactiveStreamHandler.getFluxStream(task.getData().getRegions())
            .flatMap(instrumentIntegrationService::createRegion)
            .onErrorResume(PortfolioBundleException.class,
                ReactiveStreamHandler.error(task, REGION_ENTITY, INSERT_REGIONS))
            .collectList()
            .map(o -> task);
    }

}

