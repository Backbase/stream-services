package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.CurrencyApi;
import com.backbase.investment.api.service.v1.model.Currency;
import com.backbase.investment.api.service.v1.model.CurrencyRequest;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InvestmentCurrencyService {

    public static final int CONTENT_RETRIEVE_LIMIT = 100;
    private final CurrencyApi currencyApi;

    /**
     * Upserts a batch of currency entries. For each currency, checks if it exists by code and updates it, otherwise
     * creates a new entry. Continues processing remaining entries even if individual entries fail.
     *
     * @param currencies List of currencies to upsert
     * @return Mono that completes when all currencies have been processed
     */
    public Mono<List<Currency>> upsertCurrencies(List<Currency> currencies) {
        log.info("Starting currency upsert batch operation: totalEntries={}", currencies.size());
        log.debug("Currency upsert batch details: entries={}", currencies);

        return Flux.fromIterable(currencies)
            .flatMap(this::upsertSingleCurrency)
            .doOnComplete(() -> log.info("Currency upsert batch completed successfully: totalEntriesProcessed={}",
                currencies.size()))
            .doOnError(
                error -> log.error("Currency upsert batch failed: totalEntries={}, errorType={}, errorMessage={}",
                    currencies.size(), error.getClass().getSimpleName(), error.getMessage(), error))
            .collectList();
    }

    /**
     * Upserts a single currency entry using the CurrencyApi endpoints. Implementation follows the upsert pattern:
     * <ol>
     *   <li>List existing currencies to check if the currency code already exists</li>
     *   <li>If currency exists, update it with PUT</li>
     *   <li>If not found, create a new currency entry</li>
     * </ol>
     *
     * @param currency The currency to upsert
     * @return Mono that completes with the currency when processed, or empty if validation fails
     */
    private Mono<Currency> upsertSingleCurrency(Currency currency) {
        log.debug("Processing currency: code='{}'", currency.getCode());

        // Validation
        if (currency.getCode() == null || currency.getCode().isBlank()) {
            log.warn("Skipping currency with empty code");
            return Mono.empty();
        }

        log.debug("Checking if currency exists: code='{}'", currency.getCode());

        // Check if currency already exists
        return currencyApi.listCurrencies(CONTENT_RETRIEVE_LIMIT, 0)
            .map(paginatedList -> paginatedList.getResults().stream()
                .filter(Objects::nonNull)
                .filter(entry -> currency.getCode().equals(entry.getCode()))
                .findFirst())
            .flatMap(existingCurrency -> {
                if (existingCurrency.isPresent()) {
                    log.info("Currency already exists: code='{}', updating", currency.getCode());
                    return updateCurrency(currency);
                } else {
                    log.debug("Creating new currency: code='{}'", currency.getCode());
                    return createCurrency(currency);
                }
            })
            .doOnSuccess(curr -> log.info("Currency upsert completed successfully: code='{}'", curr.getCode()))
            .doOnError(error -> log.error(
                "Currency upsert failed: code='{}', errorType={}, errorMessage={}",
                currency.getCode(), error.getClass().getSimpleName(), error.getMessage(), error))
            .onErrorResume(error -> {
                log.warn("Continuing without currency: code='{}', reason={}",
                    currency.getCode(), error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Creates a new currency entry using the CurrencyApi.
     *
     * @param currency The currency to create
     * @return Mono of the created currency
     */
    private Mono<Currency> createCurrency(Currency currency) {
        CurrencyRequest currencyRequest = new CurrencyRequest();
        currencyRequest.code(currency.getCode());
        currencyRequest.name(currency.getName());
        currencyRequest.symbol(currency.getSymbol());
        return currencyApi.createCurrency(currencyRequest)
            .doOnSuccess(created -> log.info(
                "Currency created successfully: code='{}', name='{}'",
                created.getCode(), created.getName()))
            .doOnError(error -> log.error(
                "Currency creation failed: code='{}', errorType={}, errorMessage={}",
                currency.getCode(), error.getClass().getSimpleName(), error.getMessage(), error));
    }

    /**
     * Updates an existing currency entry with new values.
     *
     * @param currency The currency with updated values
     * @return Mono of the updated currency
     */
    private Mono<Currency> updateCurrency(Currency currency) {
        CurrencyRequest currencyRequest = new CurrencyRequest();
        currencyRequest.code(currency.getCode());
        currencyRequest.name(currency.getName());
        currencyRequest.symbol(currency.getSymbol());
        return currencyApi.updateCurrency(currency.getCode(), currencyRequest)
            .doOnSuccess(updated -> log.info(
                "Currency updated successfully: code='{}', name='{}'",
                updated.getCode(), updated.getName()))
            .doOnError(error -> log.error(
                "Currency update failed: code='{}', errorType={}, errorMessage={}",
                currency.getCode(), error.getClass().getSimpleName(), error.getMessage(), error));
    }

}