package com.backbase.stream.investment.saga;

import com.backbase.investment.api.service.v1.model.ClientCreateRequest;
import com.backbase.investment.api.service.v1.model.Status836Enum;
import com.backbase.stream.configuration.InvestmentSagaConfigurationProperties;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.service.InvestmentClientService;
import com.backbase.stream.investment.service.InvestmentPortfolioService;
import com.backbase.stream.worker.StreamTaskExecutor;
import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.StreamTask.State;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Saga orchestrating investment client ingestion (create, patch, update) using {@link InvestmentClientService}. Focuses
 * on idempotent create (optional) and safe patch/update operations, writing progress to the {@link StreamTask} history
 * for observability.
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentSaga implements StreamTaskExecutor<InvestmentTask> {

    public static final String INVESTMENT = "investment-client";
    public static final String OP_CREATE = "create";
    public static final String OP_PATCH = "patch";
    public static final String OP_UPDATE = "update";
    public static final String RESULT_CREATED = "created";
    public static final String RESULT_SKIPPED = "skipped";
    public static final String RESULT_PATCHED = "patched";
    public static final String RESULT_UPDATED = "updated";
    public static final String RESULT_FAILED = "failed";

    private final InvestmentClientService clientService;
    private final InvestmentPortfolioService investmentPortfolioService;
    private final InvestmentSagaConfigurationProperties properties;
    private boolean enabled;

    @Override
    public Mono<InvestmentTask> executeTask(InvestmentTask streamTask) {

        return upsertClients(streamTask)
            .flatMap(this::upsertInvestmentProducts)
            .flatMap(this::upsertInvestmentPortfolios)
            ;

    }

    private Mono<InvestmentTask> upsertInvestmentPortfolios(InvestmentTask investmentTask) {
        Map<String, List<UUID>> clientsByLeExternalId = investmentTask.getData().getClientsByLeExternalId();
        return Flux.fromIterable(investmentTask.getData().getInvestmentArrangements())
            .flatMap(a -> investmentPortfolioService.upsertInvestmentPortfolios(a, clientsByLeExternalId))
            .collectList()
            .map(l -> investmentTask);
    }

    private Mono<InvestmentTask> upsertInvestmentProducts(InvestmentTask investmentTask) {
        InvestmentData data = investmentTask.getData();
        return Flux.fromIterable(data.getInvestmentArrangements())
            .flatMap(investmentPortfolioService::upsertInvestmentProducts)
            .collectList()
            .map(l -> investmentTask);
    }

    public Mono<InvestmentTask> upsertClients(InvestmentTask streamTask) {
        InvestmentData investmentData = streamTask.getData();

        streamTask.info(INVESTMENT, "upsert", null, streamTask.getName(), streamTask.getId(),
            "Process Investment Clients");
        streamTask.setState(State.IN_PROGRESS);
        return Flux.fromIterable(investmentData.getClientUsers())
            .flatMap(clientUser -> clientService.upsertClient(new ClientCreateRequest()
                .internalUserId(clientUser.getInternalUserId())
                .status(Status836Enum.ACTIVE)
                .putExtraDataItem("user_external_id", clientUser.getExternalUserId())
                .putExtraDataItem("keycloak_username", clientUser.getExternalUserId())
            , clientUser.getLegalEntityExternalId()))
            .collectList()
            .map(clients -> {
                streamTask.data(clients);
                streamTask.info(INVESTMENT, "upsert", RESULT_CREATED, streamTask.getName(), streamTask.getId(),
                    "Upserted " + clients.size() + " Investment Clients");
                streamTask.setState(State.COMPLETED);
                return streamTask;
            });

    }

    @Override
    public Mono<InvestmentTask> rollBack(InvestmentTask streamTask) {
        return null;
    }

    public boolean isEnabled() {
        return true;//enabled;
    }

}

