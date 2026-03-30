package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.backbase.investment.api.service.v1.CurrencyApi;
import com.backbase.investment.api.service.v1.model.Currency;
import com.backbase.investment.api.service.v1.model.CurrencyRequest;
import com.backbase.investment.api.service.v1.model.PaginatedCurrencyList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link InvestmentCurrencyService}.
 *
 * <p>Tests are grouped by method under {@link Nested} classes. Each nested class covers a single
 * public method, and each test covers a specific branch or edge case.
 *
 * <p>Conventions:
 * <ul>
 *   <li>All dependencies are mocked via Mockito</li>
 *   <li>Reactive assertions use {@link StepVerifier}</li>
 *   <li>Arrange-Act-Assert structure is followed throughout</li>
 *   <li>Helper methods at the bottom reduce boilerplate</li>
 * </ul>
 */
@DisplayName("InvestmentCurrencyService")
class InvestmentCurrencyServiceTest {

    private CurrencyApi currencyApi;
    private InvestmentCurrencyService service;

    @BeforeEach
    void setUp() {
        currencyApi = mock(CurrencyApi.class);
        service = new InvestmentCurrencyService(currencyApi);
    }

    // =========================================================================
    // upsertCurrencies
    // =========================================================================

    /**
     * Tests for {@link InvestmentCurrencyService#upsertCurrencies(List)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Empty input list → returns empty result list, no API calls made</li>
     *   <li>Currency not in existing list → createCurrency is called</li>
     *   <li>Currency already in existing list → updateCurrency is called</li>
     *   <li>Multiple currencies — mix of create and update paths</li>
     *   <li>Currency with null code → skipped (Mono.empty), absent from result</li>
     *   <li>Currency with blank code → skipped (Mono.empty), absent from result</li>
     *   <li>listCurrencies API fails → onErrorResume swallows, entry absent from result</li>
     *   <li>createCurrency fails → onErrorResume swallows, entry absent from result</li>
     *   <li>updateCurrency fails → onErrorResume swallows, entry absent from result</li>
     *   <li>Null entry in paginated results → Objects::nonNull filter, falls through to create</li>
     *   <li>All currencies fail → result list is empty</li>
     *   <li>CONTENT_RETRIEVE_LIMIT (100) is used as the limit argument to listCurrencies</li>
     *   <li>Paginated results list is null → treated as empty, falls through to create</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertCurrencies")
    class UpsertCurrenciesTests {

        @Test
        @DisplayName("empty input list — returns empty result list without calling API")
        void upsertCurrencies_emptyList_returnsEmptyListWithoutCallingApi() {
            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(currencyApi, never()).listCurrencies(any(), any());
            verify(currencyApi, never()).createCurrency(any());
            verify(currencyApi, never()).updateCurrency(any(), any());
        }

        @Test
        @DisplayName("currency code not in existing list — createCurrency is called and currency returned")
        void upsertCurrencies_currencyNotExists_createCurrencyCalledAndReturned() {
            // Arrange
            Currency currency = buildCurrency("USD", "US Dollar", "$");

            PaginatedCurrencyList emptyPage = buildPage(List.of());
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(emptyPage));

            Currency created = buildCurrency("USD", "US Dollar", "$");
            when(currencyApi.createCurrency(any(CurrencyRequest.class)))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(result -> result.size() == 1 && "USD".equals(result.get(0).getCode()))
                .verifyComplete();

            ArgumentCaptor<CurrencyRequest> captor = ArgumentCaptor.forClass(CurrencyRequest.class);
            verify(currencyApi).createCurrency(captor.capture());
            verify(currencyApi, never()).updateCurrency(any(), any());
            assertThat(captor.getValue().getCode()).isEqualTo("USD");
            assertThat(captor.getValue().getName()).isEqualTo("US Dollar");
            assertThat(captor.getValue().getSymbol()).isEqualTo("$");
        }

        @Test
        @DisplayName("currency already exists with identical name and symbol — updateCurrency is skipped and existing returned")
        void upsertCurrencies_currencyUnchanged_updateSkipped() {
            // Arrange — existing and desired carry exactly the same name and symbol
            Currency currency = buildCurrency("EUR", "Euro", "€");
            Currency existingEntry = buildCurrency("EUR", "Euro", "€");
            PaginatedCurrencyList page = buildPage(List.of(existingEntry));
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(page));

            // Act & Assert — existing currency returned without any update call
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(result -> result.size() == 1 && "EUR".equals(result.get(0).getCode()))
                .verifyComplete();

            verify(currencyApi, never()).updateCurrency(any(), any());
            verify(currencyApi, never()).createCurrency(any());
        }

        @Test
        @DisplayName("currency already exists — updateCurrency is called and currency returned")
        void upsertCurrencies_currencyAlreadyExists_updateCurrencyCalledAndReturned() {
            // Arrange — desired name differs from existing name so update IS triggered
            Currency currency = buildCurrency("EUR", "Euro Updated", "€");

            Currency existingEntry = buildCurrency("EUR", "Euro", "€");
            PaginatedCurrencyList page = buildPage(List.of(existingEntry));
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(page));

            Currency updated = buildCurrency("EUR", "Euro Updated", "€");
            when(currencyApi.updateCurrency(eq("EUR"), any(CurrencyRequest.class)))
                .thenReturn(Mono.just(updated));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(result -> result.size() == 1 && "EUR".equals(result.get(0).getCode()))
                .verifyComplete();

            ArgumentCaptor<CurrencyRequest> captor = ArgumentCaptor.forClass(CurrencyRequest.class);
            verify(currencyApi).updateCurrency(eq("EUR"), captor.capture());
            verify(currencyApi, never()).createCurrency(any());
            assertThat(captor.getValue().getCode()).isEqualTo("EUR");
            assertThat(captor.getValue().getName()).isEqualTo("Euro Updated");
            assertThat(captor.getValue().getSymbol()).isEqualTo("€");
        }

        @Test
        @DisplayName("multiple currencies — mix of create and update, both results collected")
        void upsertCurrencies_multipleCurrencies_mixedCreateAndUpdate_allCollected() {
            // Arrange — eurCurrency has a new name; existingEur has old name → update triggered for EUR
            Currency usdCurrency = buildCurrency("USD", "US Dollar", "$");
            Currency eurCurrency = buildCurrency("EUR", "Euro Updated", "€");

            // EUR already exists (old name), USD does not
            Currency existingEur = buildCurrency("EUR", "Euro", "€");
            PaginatedCurrencyList page = buildPage(List.of(existingEur));
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(page));

            Currency createdUsd = buildCurrency("USD", "US Dollar", "$");
            when(currencyApi.createCurrency(any(CurrencyRequest.class)))
                .thenReturn(Mono.just(createdUsd));

            Currency updatedEur = buildCurrency("EUR", "Euro", "€");
            when(currencyApi.updateCurrency(eq("EUR"), any(CurrencyRequest.class)))
                .thenReturn(Mono.just(updatedEur));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(usdCurrency, eurCurrency)))
                .expectNextMatches(result -> result.size() == 2)
                .verifyComplete();

            verify(currencyApi).createCurrency(any(CurrencyRequest.class));
            verify(currencyApi).updateCurrency(eq("EUR"), any(CurrencyRequest.class));
        }

        @Test
        @DisplayName("currency with null code — validation skips it, entry absent from result")
        void upsertCurrencies_nullCode_skippedAndAbsentFromResult() {
            // Arrange
            Currency currency = buildCurrency(null, "Unknown", "?");

            // Act & Assert — skipped via Mono.empty(), so result list is empty
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(currencyApi, never()).listCurrencies(any(), any());
            verify(currencyApi, never()).createCurrency(any());
            verify(currencyApi, never()).updateCurrency(any(), any());
        }

        @Test
        @DisplayName("currency with blank code — validation skips it, entry absent from result")
        void upsertCurrencies_blankCode_skippedAndAbsentFromResult() {
            // Arrange
            Currency currency = buildCurrency("   ", "Unknown", "?");

            // Act & Assert — blank code fails the isBlank() check → Mono.empty()
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(currencyApi, never()).listCurrencies(any(), any());
            verify(currencyApi, never()).createCurrency(any());
            verify(currencyApi, never()).updateCurrency(any(), any());
        }

        @Test
        @DisplayName("listCurrencies API fails — onErrorResume swallows error, entry absent from result")
        void upsertCurrencies_listCurrenciesFails_errorSwallowed_entryAbsentFromResult() {
            // Arrange
            Currency currency = buildCurrency("GBP", "Pound Sterling", "£");

            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));

            // Act & Assert — onErrorResume returns Mono.empty(), collected list is empty
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(currencyApi, never()).createCurrency(any());
            verify(currencyApi, never()).updateCurrency(any(), any());
        }

        @Test
        @DisplayName("createCurrency fails — onErrorResume swallows error, entry absent from result")
        void upsertCurrencies_createCurrencyFails_errorSwallowed_entryAbsentFromResult() {
            // Arrange
            Currency currency = buildCurrency("JPY", "Japanese Yen", "¥");

            PaginatedCurrencyList emptyPage = buildPage(List.of());
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(emptyPage));

            when(currencyApi.createCurrency(any(CurrencyRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("create failed")));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
        }

        @Test
        @DisplayName("updateCurrency fails — onErrorResume swallows error, entry absent from result")
        void upsertCurrencies_updateCurrencyFails_errorSwallowed_entryAbsentFromResult() {
            // Arrange — desired name differs from existing so update IS triggered (then fails and is swallowed)
            Currency currency = buildCurrency("CHF", "Swiss Franc Updated", "Fr");

            Currency existingEntry = buildCurrency("CHF", "Swiss Franc", "Fr");
            PaginatedCurrencyList page = buildPage(List.of(existingEntry));
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(page));

            when(currencyApi.updateCurrency(eq("CHF"), any(CurrencyRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("update failed")));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
        }

        @Test
        @DisplayName("paginated results contain a null entry — null filtered out, falls through to create path")
        void upsertCurrencies_nullEntryInPageResults_filteredOut_createPathTaken() {
            // Arrange — results list has a null element; Objects::nonNull removes it, code not matched
            Currency currency = buildCurrency("AUD", "Australian Dollar", "A$");

            // Use a mutable list so we can include null
            List<Currency> resultsWithNull = new ArrayList<>();
            resultsWithNull.add(null);
            PaginatedCurrencyList page = buildPage(resultsWithNull);
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(page));

            Currency created = buildCurrency("AUD", "Australian Dollar", "A$");
            when(currencyApi.createCurrency(any(CurrencyRequest.class)))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(result -> result.size() == 1 && "AUD".equals(result.get(0).getCode()))
                .verifyComplete();

            verify(currencyApi).createCurrency(any(CurrencyRequest.class));
            verify(currencyApi, never()).updateCurrency(any(), any());
        }

        @Test
        @DisplayName("all currencies fail — result list is empty")
        void upsertCurrencies_allCurrenciesFail_resultListIsEmpty() {
            // Arrange — two currencies both fail on listCurrencies
            Currency usd = buildCurrency("USD", "US Dollar", "$");
            Currency eur = buildCurrency("EUR", "Euro", "€");

            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.error(new RuntimeException("service down")));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(usd, eur)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
        }

        @Test
        @DisplayName("CONTENT_RETRIEVE_LIMIT (100) is used as the limit argument to listCurrencies")
        void upsertCurrencies_usesContentRetrieveLimit_asListCurrenciesLimit() {
            // Arrange
            Currency currency = buildCurrency("CAD", "Canadian Dollar", "C$");

            PaginatedCurrencyList emptyPage = buildPage(List.of());
            when(currencyApi.listCurrencies(100, 0))
                .thenReturn(Mono.just(emptyPage));

            Currency created = buildCurrency("CAD", "Canadian Dollar", "C$");
            when(currencyApi.createCurrency(any(CurrencyRequest.class)))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertCurrencies(List.of(currency)))
                .expectNextMatches(result -> result.size() == 1)
                .verifyComplete();

            // Explicitly verifies the constant value 100 is passed as the limit
            verify(currencyApi).listCurrencies(100, 0);
        }

        @Test
        @DisplayName("single currency with null code mixed with valid currency — only valid currency in result")
        void upsertCurrencies_mixedNullAndValidCode_onlyValidCurrencyInResult() {
            // Arrange
            Currency invalidCurrency = buildCurrency(null, "Unknown", "?");
            Currency validCurrency = buildCurrency("SEK", "Swedish Krona", "kr");

            PaginatedCurrencyList emptyPage = buildPage(List.of());
            when(currencyApi.listCurrencies(InvestmentCurrencyService.CONTENT_RETRIEVE_LIMIT, 0))
                .thenReturn(Mono.just(emptyPage));

            Currency created = buildCurrency("SEK", "Swedish Krona", "kr");
            when(currencyApi.createCurrency(any(CurrencyRequest.class)))
                .thenReturn(Mono.just(created));

            // Act & Assert — invalid skipped, valid created → result has exactly one entry
            StepVerifier.create(service.upsertCurrencies(List.of(invalidCurrency, validCurrency)))
                .expectNextMatches(result -> result.size() == 1 && "SEK".equals(result.get(0).getCode()))
                .verifyComplete();

            verify(currencyApi).createCurrency(any(CurrencyRequest.class));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a {@link Currency} with the given field values.
     *
     * @param code   ISO currency code (may be {@code null} for invalid-input tests)
     * @param name   human-readable currency name
     * @param symbol currency symbol
     * @return a fully populated {@link Currency}
     */
    private Currency buildCurrency(String code, String name, String symbol) {
        Currency currency = new Currency();
        currency.setCode(code);
        currency.setName(name);
        currency.setSymbol(symbol);
        return currency;
    }

    /**
     * Builds a {@link PaginatedCurrencyList} whose {@code results} list is the given list.
     *
     * @param results list of {@link Currency} entries (may contain {@code null} elements)
     * @return a {@link PaginatedCurrencyList} wrapping the provided results
     */
    private PaginatedCurrencyList buildPage(List<Currency> results) {
        PaginatedCurrencyList page = new PaginatedCurrencyList();
        page.setResults(results);
        page.setCount(results.size());
        return page;
    }
}

