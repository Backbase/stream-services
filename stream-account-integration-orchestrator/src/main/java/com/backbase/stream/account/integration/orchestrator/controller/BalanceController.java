package com.backbase.stream.account.integration.orchestrator.controller;

import com.backbase.dbs.accounts.presentation.service.model.ArrangementItem;
import com.backbase.stream.account.integration.orchestrator.service.ApiSelector;
import com.backbase.stream.dbs.account.integration.inbound.api.ArrangementDetailsApi;
import com.backbase.stream.dbs.account.integration.inbound.api.BalancesApi;
import com.backbase.stream.dbs.account.integration.model.ArrangementDetails;
import com.backbase.stream.dbs.account.integration.model.BalanceItemItem;
import com.backbase.stream.product.configuration.ProductConfiguration;
import com.backbase.stream.product.service.ArrangementService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@RestController
@Import(ProductConfiguration.class)
public class BalanceController implements BalancesApi, ArrangementDetailsApi {

    private final ArrangementService arrangementService;

    private final ApiSelector apiSelector;

    @Override
    public Mono<ResponseEntity<Flux<BalanceItemItem>>> getBalance(@NotNull @Valid String arrangementIds, ServerWebExchange exchange) {

        return Mono.just(ResponseEntity.ok(arrangementService.getArrangements(Arrays.asList(arrangementIds.split(",")))
            .groupBy(ArrangementItem::getExternalProductId)
            .doOnNext(productTypeArrangements -> log.info("Getting balances for type: {}", productTypeArrangements.key()))
            .flatMap(productTypeArrangements -> productTypeArrangements.collectList()
                .flatMapMany(arrangementItems -> getBalanceFromApiSelector(productTypeArrangements.key(), arrangementItems)))));

    }

    private Flux<BalanceItemItem> getBalanceFromApiSelector(String productType, List<ArrangementItem> arrangementItems) {
        return apiSelector.balancesApi(productType).getBalance(getArrangementIds(arrangementItems));
    }


    private String getArrangementIds(List<ArrangementItem> arrangementItems) {
        return arrangementItems.stream()
            .map(ArrangementItem::getExternalArrangementId)
            .collect(Collectors.joining(","));
    }

    @Override
    public Mono<ResponseEntity<ArrangementDetails>> getArrangementDetails(@NotNull @Valid String arrangementId, ServerWebExchange exchange) {

        return arrangementService.getArrangementByExternalId(arrangementId).flatMap(arrangementItem ->
            apiSelector.arrangementDetailsApi(arrangementItem.getExternalProductId()).getArrangementDetails(arrangementId))
            .map(ResponseEntity::ok);

    }
}
