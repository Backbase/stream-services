package com.backbase.stream.investment.saga;

import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.Status836Enum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentModelPortfolioService;
import com.backbase.stream.investment.service.InvestmentPortfolioAllocationService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.StreamTask.State;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
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
public class InvestmentSaga implements StreamTaskExecutor<InvestmentTask> {

    public static final String INVESTMENT = "investment-client";
    public static final String OP_UPSERT = "upsert";
    public static final String RESULT_CREATED = "created";
    public static final String RESULT_FAILED = "failed";

    private static final String INVESTMENT_PRODUCTS = "investment-products";
    private static final String INVESTMENT_PORTFOLIO_MODELS = "investment-portfolio-models";
    private static final String INVESTMENT_PORTFOLIOS = "investment-portfolios";
    private static final String PROCESSING_PREFIX = "Processing ";
    private static final String UPSERTED_PREFIX = "Upserted ";

    private final InvestmentClientService clientService;
    private final InvestmentPortfolioService investmentPortfolioService;
    private final InvestmentPortfolioAllocationService investmentPortfolioAllocationService;
    private final InvestmentModelPortfolioService investmentModelPortfolioService;
    private final InvestmentIngestionConfigurationProperties coreConfigurationProperties;

    @Override
    public Mono<InvestmentTask> executeTask(InvestmentTask streamTask) {
        if (!coreConfigurationProperties.isWealthEnabled()) {
            log.warn("Skip investment saga execution: taskId={}, taskName={}",
                streamTask.getId(), streamTask.getName());
            return Mono.just(streamTask);
        }
        log.info("Starting investment saga execution: taskId={}, taskName={}",
            streamTask.getId(), streamTask.getName());
        return this.upsertInvestmentPortfolioModels(streamTask)
            .flatMap(this::upsertClients)
            .flatMap(this::upsertInvestmentProducts)
            .flatMap(this::upsertInvestmentPortfolios)
            .flatMap(this::upsertInvestmentPortfolioDeposits)
            .flatMap(this::upsertPortfoliosAllocations)
            .doOnSuccess(completedTask -> log.info(
                "Successfully completed investment saga: taskId={}, taskName={}, state={}",
                completedTask.getId(), completedTask.getName(), completedTask.getState()))
            .doOnError(throwable -> {
                log.error("Failed to execute investment saga: taskId={}, taskName={}",
                    streamTask.getId(), streamTask.getName(), throwable);
                streamTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED,
                    streamTask.getName(), streamTask.getId(),
                    "Investment saga failed: " + throwable.getMessage());
                streamTask.setState(State.FAILED);
            })
            .onErrorResume(throwable -> Mono.just(streamTask));
    }

    private Mono<InvestmentTask> upsertInvestmentPortfolioDeposits(InvestmentTask investmentTask) {
        return Flux.fromIterable(Objects.requireNonNullElse(investmentTask.getData().getPortfolios(), List.of()))
            .flatMap(investmentPortfolioService::upsertDeposits)
            .onErrorResume(throwable -> {
                log.warn("Failed to create deposit for portfolio", throwable);
                return Mono.empty();
            })
            .flatMap(investmentPortfolioAllocationService::createDepositAllocation)
            .collectList()
            .map(o -> investmentTask);
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
    public Mono<InvestmentTask> rollBack(InvestmentTask streamTask) {
        log.warn("Rollback requested for investment saga but not implemented: taskId={}, taskName={}",
            streamTask.getId(), streamTask.getName());
        return Mono.empty();
    }

    private Mono<InvestmentTask> upsertPortfoliosAllocations(InvestmentTask investmentTask) {
        InvestmentData data = investmentTask.getData();
        return Flux.fromIterable(Objects.requireNonNullElse(data.getPortfolios(), List.of()))
            .flatMap(
                p -> investmentPortfolioAllocationService.generateAllocations(p,
                    data.getPortfolioProducts(),
                    investmentTask.getData().getInvestmentAssetData()))
            .collectList()
            .map(o -> investmentTask);
    }

    /**
     * Upserts investment portfolios for all investment arrangements.
     *
     * <p>This method creates or updates portfolios and associates them with investment clients.
     * Client associations are resolved from legal entity external IDs using the clientsByLeExternalId map populated in
     * earlier steps.
     *
     * <p>Each portfolio is created with:
     * <ul>
     *   <li>Product UUID from the previous step</li>
     *   <li>Arrangement details (ID, external ID, name)</li>
     *   <li>Associated client UUIDs</li>
     *   <li>Default currency (EUR) and ACTIVE status</li>
     * </ul>
     *
     * @param investmentTask the task containing investment arrangements to process
     * @return Mono emitting the task with completed state
     */
    private Mono<InvestmentTask> upsertInvestmentPortfolios(InvestmentTask investmentTask) {
        Map<String, List<UUID>> clientsByLeExternalId = investmentTask.getData().getClientsByLeExternalId();
        int arrangementCount = investmentTask.getData().getInvestmentArrangements().size();

        log.info("Starting investment portfolio upsert: taskId={}, arrangementCount={}, legalEntityCount={}",
            investmentTask.getId(), arrangementCount, clientsByLeExternalId.size());

        investmentTask.info(INVESTMENT_PORTFOLIOS, OP_UPSERT, null, investmentTask.getName(),
            investmentTask.getId(), PROCESSING_PREFIX + arrangementCount + " investment portfolios");

        return Flux.fromIterable(investmentTask.getData().getInvestmentArrangements())
            .flatMap(arrangement -> {
                log.debug("Upserting investment portfolio for arrangement: externalId={}, name={}, productId={}",
                    arrangement.getExternalId(), arrangement.getName(), arrangement.getInvestmentProductId());

                return investmentPortfolioService.upsertInvestmentPortfolios(arrangement, clientsByLeExternalId)
                    .doOnSuccess(portfolio -> log.debug(
                        "Successfully upserted investment portfolio: portfolioUuid={}, externalId={}, name={}",
                        portfolio.getUuid(), portfolio.getExternalId(), portfolio.getName()))
                    .doOnError(throwable -> log.error(
                        "Failed to upsert investment portfolio: arrangementExternalId={}, arrangementName={}",
                        arrangement.getExternalId(), arrangement.getName(), throwable));
            })
            .collectList()
            .map(portfolios -> {
                investmentTask.info(INVESTMENT_PORTFOLIOS, OP_UPSERT, RESULT_CREATED,
                    investmentTask.getName(), investmentTask.getId(),
                    UPSERTED_PREFIX + portfolios.size() + " investment portfolios");
                investmentTask.setState(State.COMPLETED);
                investmentTask.setPortfolios(portfolios);

                log.info("Successfully upserted all investment portfolios: taskId={}, portfolioCount={}",
                    investmentTask.getId(), portfolios.size());

                return investmentTask;
            })
            .doOnError(throwable -> {
                log.error("Failed to upsert investment portfolios: taskId={}, arrangementCount={}",
                    investmentTask.getId(), arrangementCount, throwable);
                investmentTask.error(INVESTMENT_PORTFOLIOS, OP_UPSERT, RESULT_FAILED,
                    investmentTask.getName(), investmentTask.getId(),
                    "Failed to upsert investment portfolios: " + throwable.getMessage());
                investmentTask.setState(State.FAILED);
            });
    }

    private Mono<InvestmentTask> upsertInvestmentPortfolioModels(InvestmentTask investmentTask) {
        InvestmentData data = investmentTask.getData();
        int arrangementCount = data.getInvestmentArrangements().size();

        log.info("Starting investment portfolio models upsert: taskId={}, arrangementCount={}",
            investmentTask.getId(), arrangementCount);

        investmentTask.info(INVESTMENT_PORTFOLIO_MODELS, OP_UPSERT, null, investmentTask.getName(),
            investmentTask.getId(), PROCESSING_PREFIX + arrangementCount + " investment portfolio models");

        return investmentModelPortfolioService.upsertModels(data)
            .collectList()
            .map(modelPortfolio -> {
                investmentTask.info(INVESTMENT_PORTFOLIO_MODELS, OP_UPSERT, RESULT_CREATED,
                    investmentTask.getName(), investmentTask.getId(),
                    UPSERTED_PREFIX + modelPortfolio.size() + " investment portfolio models");

                log.info("Successfully upserted all investment portfolio models: taskId={}, productCount={}",
                    investmentTask.getId(), modelPortfolio.size());

                return investmentTask;
            })
            .doOnError(throwable -> {
                log.error("Failed to upsert investment portfolio models: taskId={}, arrangementCount={}",
                    investmentTask.getId(), arrangementCount, throwable);
                investmentTask.error(INVESTMENT_PORTFOLIO_MODELS, OP_UPSERT, RESULT_FAILED,
                    investmentTask.getName(), investmentTask.getId(),
                    "Failed to upsert investment portfolio models: " + throwable.getMessage());
            });
    }

    /**
     * Upserts investment products for all investment arrangements.
     *
     * <p>This method creates or updates portfolio products (typically SELF_TRADING type)
     * for each investment arrangement. The products are created in parallel and collected into a list before proceeding
     * to the next saga step.
     *
     * <p>Product UUIDs are stored in the arrangement objects for use in portfolio creation.
     *
     * @param investmentTask the task containing investment arrangements to process
     * @return Mono emitting the task unchanged (product IDs are updated in arrangements)
     */
    private Mono<InvestmentTask> upsertInvestmentProducts(InvestmentTask investmentTask) {
        InvestmentData data = investmentTask.getData();
        int arrangementCount = data.getInvestmentArrangements().size();

        log.info("Starting investment product upsert: taskId={}, arrangementCount={}",
            investmentTask.getId(), arrangementCount);

        investmentTask.info(INVESTMENT_PRODUCTS, OP_UPSERT, null, investmentTask.getName(),
            investmentTask.getId(), PROCESSING_PREFIX + arrangementCount + " investment products");

        return Mono.just(data.getInvestmentArrangements())
            .flatMap(arrangements -> investmentPortfolioService.upsertInvestmentProducts(data, arrangements))
            .map(products -> {
                investmentTask.info(INVESTMENT_PRODUCTS, OP_UPSERT, RESULT_CREATED,
                    investmentTask.getName(), investmentTask.getId(),
                    UPSERTED_PREFIX + products.size() + " investment products");
                data.setPortfoliosProducts(products);
                log.info("Successfully upserted all investment products: taskId={}, productCount={}",
                    investmentTask.getId(), products.size());

                return investmentTask;
            })
            .doOnError(throwable -> {
                log.error("Failed to upsert investment products: taskId={}, arrangementCount={}",
                    investmentTask.getId(), arrangementCount, throwable);
                investmentTask.error(INVESTMENT_PRODUCTS, OP_UPSERT, RESULT_FAILED,
                    investmentTask.getName(), investmentTask.getId(),
                    "Failed to upsert investment products: " + throwable.getMessage());
            });
    }

    /**
     * Upserts investment clients for all users in the investment data.
     *
     * <p>This method:
     * <ol>
     *   <li>Updates task state to IN_PROGRESS</li>
     *   <li>Creates client requests with user IDs and metadata</li>
     *   <li>Processes all clients in parallel using Flux</li>
     *   <li>Collects results and updates task data</li>
     *   <li>Updates task state to COMPLETED on success</li>
     * </ol>
     *
     * <p>Each client is created with:
     * <ul>
     *   <li>Internal user ID for system integration</li>
     *   <li>External user ID in extra data for reference</li>
     *   <li>Keycloak username for authentication integration</li>
     *   <li>Active status</li>
     * </ul>
     *
     * @param streamTask the task containing client users to process
     * @return Mono emitting the task with updated client data and state
     */
    private Mono<InvestmentTask> upsertClients(InvestmentTask streamTask) {
        InvestmentData investmentData = streamTask.getData();
        int clientCount = investmentData.getClientUsers().size();

        log.info("Starting client upsert: taskId={}, clientCount={}", streamTask.getId(), clientCount);

        streamTask.info(INVESTMENT, OP_UPSERT, null, streamTask.getName(), streamTask.getId(),
            PROCESSING_PREFIX + clientCount + " investment clients");
        streamTask.setState(State.IN_PROGRESS);

        return Flux.fromIterable(investmentData.getClientUsers())
            .flatMap(clientUser -> {
                log.debug("Upserting investment client: internalUserId={}, externalUserId={}, legalEntityExternalId={}",
                    clientUser.getInternalUserId(), clientUser.getExternalUserId(),
                    clientUser.getLegalEntityExternalId());

                ClientCreateRequest request = new ClientCreateRequest()
                    .internalUserId(clientUser.getInternalUserId())
                    .status(Status836Enum.ACTIVE)
                    .putExtraDataItem("user_external_id", clientUser.getExternalUserId())
                    .putExtraDataItem("keycloak_username", clientUser.getExternalUserId());

                return clientService.upsertClient(request, clientUser.getLegalEntityExternalId())
                    .doOnSuccess(upsertedClient -> log.debug(
                        "Successfully upserted client: investmentClientId={}, internalUserId={}",
                        upsertedClient.getInvestmentClientId(), upsertedClient.getInternalUserId()))
                    .doOnError(throwable -> log.error(
                        "Failed to upsert client: internalUserId={}, externalUserId={}, legalEntityExternalId={}",
                        clientUser.getInternalUserId(), clientUser.getExternalUserId(),
                        clientUser.getLegalEntityExternalId(), throwable));
            })
            .collectList()
            .map(clients -> {
                streamTask.data(clients);
                streamTask.info(INVESTMENT, OP_UPSERT, RESULT_CREATED, streamTask.getName(), streamTask.getId(),
                    UPSERTED_PREFIX + clients.size() + " investment clients");
                streamTask.setState(State.COMPLETED);

                log.info("Successfully upserted all clients: taskId={}, clientCount={}, successCount={}",
                    streamTask.getId(), clientCount, clients.size());

                return streamTask;
            })
            .doOnError(throwable -> {
                log.error("Failed to upsert clients: taskId={}, clientCount={}",
                    streamTask.getId(), clientCount, throwable);
                streamTask.error(INVESTMENT, OP_UPSERT, RESULT_FAILED,
                    streamTask.getName(), streamTask.getId(),
                    "Failed to upsert investment clients: " + throwable.getMessage());
                streamTask.setState(State.FAILED);
            });
    }

}

