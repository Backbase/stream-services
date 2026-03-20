package com.backbase.stream.investment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.backbase.investment.api.service.v1.model.Deposit;
import com.backbase.investment.api.service.v1.model.DepositRequest;
import com.backbase.investment.api.service.v1.model.PaginatedDepositList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioListList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioTradingAccountList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioTradingAccount;
import com.backbase.investment.api.service.v1.model.PortfolioTradingAccountRequest;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.investment.api.service.v1.model.StatusA3dEnum;
import com.backbase.stream.configuration.InvestmentIngestProperties;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.model.InvestmentPortfolioTradingAccount;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link InvestmentPortfolioService}.
 *
 * <p>Tests are grouped by method under {@link Nested} classes to improve readability
 * and navigation. Each nested class covers a single public or package-visible method,
 * and each test method covers a specific branch or edge case.
 *
 * <p>Conventions:
 * <ul>
 *   <li>All dependencies are mocked via Mockito</li>
 *   <li>Reactive assertions use {@link StepVerifier}</li>
 *   <li>Arrange-Act-Assert structure is followed throughout</li>
 *   <li>Helper methods at the bottom of the class reduce boilerplate</li>
 * </ul>
 */
@DisplayName("InvestmentPortfolioService")
class InvestmentPortfolioServiceTest {

    private InvestmentProductsApi productsApi;
    private PortfolioApi portfolioApi;
    private PaymentsApi paymentsApi;
    private PortfolioTradingAccountsApi portfolioTradingAccountsApi;
    private InvestmentIngestProperties config;
    private InvestmentPortfolioService service;

    @BeforeEach
    void setUp() {
        productsApi = Mockito.mock(InvestmentProductsApi.class);
        portfolioApi = Mockito.mock(PortfolioApi.class);
        paymentsApi = Mockito.mock(PaymentsApi.class);
        portfolioTradingAccountsApi = Mockito.mock(PortfolioTradingAccountsApi.class);
        config = Mockito.mock(InvestmentIngestProperties.class);
        when(config.getPortfolio().getActivationPastMonths()).thenReturn(6);
        service = new InvestmentPortfolioService(
            productsApi, portfolioApi, paymentsApi, portfolioTradingAccountsApi, config);
    }

    // =========================================================================
    // upsertPortfolioTradingAccount
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#upsertPortfolioTradingAccount(PortfolioTradingAccountRequest)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Existing account found → patch succeeds</li>
     *   <li>No existing account → create new</li>
     *   <li>Patch fails with {@link WebClientResponseException} → falls back to existing</li>
     *   <li>Patch fails with non-WebClient exception → error propagated</li>
     *   <li>Multiple existing accounts → {@link IllegalStateException}</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertPortfolioTradingAccount")
    class UpsertPortfolioTradingAccountTests {

        @Test
        @DisplayName("existing account found — patches and returns updated account")
        void upsertPortfolioTradingAccount_existingAccount_patchesAndReturns() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioTradingAccount existing = Mockito.mock(PortfolioTradingAccount.class);
            when(existing.getUuid()).thenReturn(existingUuid);
            when(existing.getExternalAccountId()).thenReturn("EXT-001");

            PortfolioTradingAccount patched = Mockito.mock(PortfolioTradingAccount.class);
            when(patched.getUuid()).thenReturn(existingUuid);
            when(patched.getExternalAccountId()).thenReturn("EXT-001");

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-001")
                .externalAccountId("EXT-001")
                .isDefault(true)
                .isInternal(false);

            PaginatedPortfolioTradingAccountList accountList = new PaginatedPortfolioTradingAccountList()
                .results(List.of(existing));

            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("EXT-001"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(accountList));

            when(portfolioTradingAccountsApi.patchPortfolioTradingAccount(
                existingUuid.toString(), request))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccount(request))
                .expectNextMatches(acc -> existingUuid.equals(acc.getUuid()))
                .verifyComplete();

            verify(portfolioTradingAccountsApi).patchPortfolioTradingAccount(
                existingUuid.toString(), request);
            verify(portfolioTradingAccountsApi, never()).createPortfolioTradingAccount(any());
        }

        @Test
        @DisplayName("no existing account found — creates and returns new account")
        void upsertPortfolioTradingAccount_noExistingAccount_createsNew() {
            // Arrange
            UUID newUuid = UUID.randomUUID();
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioTradingAccount created = Mockito.mock(PortfolioTradingAccount.class);
            when(created.getUuid()).thenReturn(newUuid);
            when(created.getExternalAccountId()).thenReturn("EXT-002");

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-002")
                .externalAccountId("EXT-002")
                .isDefault(false)
                .isInternal(false);

            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("EXT-002"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList().results(List.of())));

            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(request))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccount(request))
                .expectNextMatches(acc -> newUuid.equals(acc.getUuid()))
                .verifyComplete();

            verify(portfolioTradingAccountsApi).createPortfolioTradingAccount(request);
            verify(portfolioTradingAccountsApi, never()).patchPortfolioTradingAccount(any(), any());
        }

        @Test
        @DisplayName("patch fails with WebClientResponseException — falls back to existing account")
        void upsertPortfolioTradingAccount_patchFails_withWebClientException_fallsBackToExisting() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioTradingAccount existing = Mockito.mock(PortfolioTradingAccount.class);
            when(existing.getUuid()).thenReturn(existingUuid);
            when(existing.getExternalAccountId()).thenReturn("EXT-003");

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-003")
                .externalAccountId("EXT-003")
                .isDefault(false)
                .isInternal(true);

            PaginatedPortfolioTradingAccountList accountList = new PaginatedPortfolioTradingAccountList()
                .results(List.of(existing));

            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("EXT-003"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(accountList));

            when(portfolioTradingAccountsApi.patchPortfolioTradingAccount(
                existingUuid.toString(), request))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity",
                    HttpHeaders.EMPTY, "patch error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(any()))
                .thenReturn(Mono.error(new RuntimeException("should not be called")));

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccount(request))
                .expectNextMatches(acc -> existingUuid.equals(acc.getUuid()))
                .verifyComplete();

            verify(portfolioTradingAccountsApi, never()).createPortfolioTradingAccount(any());
        }

        @Test
        @DisplayName("patch fails with non-WebClient exception — propagates error")
        void upsertPortfolioTradingAccount_patchFails_withNonWebClientException_propagatesError() {
            // Arrange
            UUID existingUuid = UUID.randomUUID();
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioTradingAccount existing = Mockito.mock(PortfolioTradingAccount.class);
            when(existing.getUuid()).thenReturn(existingUuid);
            when(existing.getExternalAccountId()).thenReturn("EXT-004");

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-004")
                .externalAccountId("EXT-004")
                .isDefault(false)
                .isInternal(false);

            PaginatedPortfolioTradingAccountList accountList = new PaginatedPortfolioTradingAccountList()
                .results(List.of(existing));

            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("EXT-004"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(accountList));

            when(portfolioTradingAccountsApi.patchPortfolioTradingAccount(
                existingUuid.toString(), request))
                .thenReturn(Mono.error(new RuntimeException("Unexpected DB error")));

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccount(request))
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && e.getMessage().equals("Unexpected DB error"))
                .verify();
        }

        @Test
        @DisplayName("multiple existing accounts found — returns IllegalStateException")
        void upsertPortfolioTradingAccount_multipleExistingAccounts_returnsError() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioTradingAccount acc1 = Mockito.mock(PortfolioTradingAccount.class);
            when(acc1.getUuid()).thenReturn(UUID.randomUUID());
            PortfolioTradingAccount acc2 = Mockito.mock(PortfolioTradingAccount.class);
            when(acc2.getUuid()).thenReturn(UUID.randomUUID());

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-005")
                .externalAccountId("EXT-005")
                .isDefault(false)
                .isInternal(false);

            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("EXT-005"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList()
                    .results(List.of(acc1, acc2))));

            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(any()))
                .thenReturn(Mono.error(new RuntimeException("should not be called")));

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccount(request))
                .expectErrorMatches(IllegalStateException.class::isInstance)
                .verify();
        }
    }

    // =========================================================================
    // upsertPortfolioTradingAccounts (batch)
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#upsertPortfolioTradingAccounts(List)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Null input → empty list</li>
     *   <li>Empty input → empty list</li>
     *   <li>Null portfolioExternalId on account → account is skipped</li>
     *   <li>Single failure in batch → remaining accounts processed</li>
     *   <li>All accounts fail → empty list returned</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertPortfolioTradingAccounts")
    class UpsertPortfolioTradingAccountsTests {

        @Test
        @DisplayName("null input — returns empty list without calling API")
        void upsertPortfolioTradingAccounts_nullInput_returnsEmptyList() {
            StepVerifier.create(service.upsertPortfolioTradingAccounts(null))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verifyNoInteractions(portfolioTradingAccountsApi);
        }

        @Test
        @DisplayName("empty input — returns empty list without calling API")
        void upsertPortfolioTradingAccounts_emptyInput_returnsEmptyList() {
            StepVerifier.create(service.upsertPortfolioTradingAccounts(List.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verifyNoInteractions(portfolioTradingAccountsApi);
        }

        @Test
        @DisplayName("null portfolioExternalId on account — account is silently skipped")
        void upsertPortfolioTradingAccounts_nullPortfolioExternalId_skipsAccount() {
            // Arrange
            InvestmentPortfolioTradingAccount account = InvestmentPortfolioTradingAccount.builder()
                .accountExternalId("EXT-NULL-PORTFOLIO")
                .portfolioExternalId(null)
                .accountId("ACC-NULL")
                .isDefault(false)
                .isInternal(false)
                .build();

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccounts(List.of(account)))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verify(portfolioApi, never()).listPortfolios(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("single account fails in batch — remaining accounts still processed")
        void upsertPortfolioTradingAccounts_singleFailure_doesNotStopBatch() {
            // Arrange
            UUID portfolioUuid1 = UUID.randomUUID();
            UUID portfolioUuid2 = UUID.randomUUID();
            String externalId1 = "PORTFOLIO-EXT-001";
            String externalId2 = "PORTFOLIO-EXT-002";

            mockPortfolioFound(externalId1, portfolioUuid1);
            mockPortfolioFound(externalId2, portfolioUuid2);

            // Account 1: list returns empty → create fails
            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("ACC-FAIL-001"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList().results(List.of())));
            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(
                argThat(r -> r != null && "ACC-FAIL-001".equals(r.getExternalAccountId()))))
                .thenReturn(Mono.error(new RuntimeException("API failure for account 1")));

            // Account 2: list returns empty → create succeeds
            UUID createdUuid = UUID.randomUUID();
            PortfolioTradingAccount created = Mockito.mock(PortfolioTradingAccount.class);
            when(created.getUuid()).thenReturn(createdUuid);
            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("ACC-OK-002"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList().results(List.of())));
            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(
                argThat(r -> r != null && "ACC-OK-002".equals(r.getExternalAccountId()))))
                .thenReturn(Mono.just(created));

            List<InvestmentPortfolioTradingAccount> input = List.of(
                InvestmentPortfolioTradingAccount.builder()
                    .accountExternalId("ACC-FAIL-001")
                    .portfolioExternalId(externalId1)
                    .accountId("ACC-FAIL-001")
                    .isDefault(false)
                    .isInternal(false)
                    .build(),
                InvestmentPortfolioTradingAccount.builder()
                    .accountExternalId("ACC-OK-002")
                    .portfolioExternalId(externalId2)
                    .accountId("ACC-OK-002")
                    .isDefault(false)
                    .isInternal(false)
                    .build()
            );

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccounts(input))
                .expectNextMatches(list -> list.size() == 1
                    && createdUuid.equals(list.getFirst().getUuid()))
                .verifyComplete();
        }

        @Test
        @DisplayName("all accounts fail — returns empty list without throwing")
        void upsertPortfolioTradingAccounts_allFail_returnsEmptyList() {
            // Arrange
            String externalId = "PORTFOLIO-EXT-ALL-FAIL";
            UUID portfolioUuid = UUID.randomUUID();

            mockPortfolioFound(externalId, portfolioUuid);

            when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
                eq(1), isNull(), isNull(), eq("ACC-ALL-FAIL"), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("API failure")));

            List<InvestmentPortfolioTradingAccount> input = List.of(
                InvestmentPortfolioTradingAccount.builder()
                    .accountExternalId("ACC-ALL-FAIL")
                    .portfolioExternalId(externalId)
                    .accountId("ACC-ALL-FAIL")
                    .isDefault(false)
                    .isInternal(false)
                    .build()
            );

            // Act & Assert
            StepVerifier.create(service.upsertPortfolioTradingAccounts(input))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
        }
    }

    // =========================================================================
    // createPortfolioTradingAccount
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#createPortfolioTradingAccount(PortfolioTradingAccountRequest)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>API returns successfully → account emitted</li>
     *   <li>API throws → error propagated</li>
     * </ul>
     */
    @Nested
    @DisplayName("createPortfolioTradingAccount")
    class CreatePortfolioTradingAccountTests {

        @Test
        @DisplayName("API succeeds — returns created account")
        void createPortfolioTradingAccount_success_returnsCreatedAccount() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            UUID newUuid = UUID.randomUUID();

            PortfolioTradingAccount created = Mockito.mock(PortfolioTradingAccount.class);
            when(created.getUuid()).thenReturn(newUuid);
            when(created.getExternalAccountId()).thenReturn("EXT-NEW");

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-NEW")
                .externalAccountId("EXT-NEW")
                .isDefault(true)
                .isInternal(false);

            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(request))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.createPortfolioTradingAccount(request))
                .expectNextMatches(acc -> newUuid.equals(acc.getUuid())
                    && "EXT-NEW".equals(acc.getExternalAccountId()))
                .verifyComplete();
        }

        @Test
        @DisplayName("API fails — propagates error to caller")
        void createPortfolioTradingAccount_apiFails_propagatesError() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();

            PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
                .portfolio(portfolioUuid)
                .accountId("ACC-ERR")
                .externalAccountId("EXT-ERR")
                .isDefault(false)
                .isInternal(false);

            when(portfolioTradingAccountsApi.createPortfolioTradingAccount(request))
                .thenReturn(Mono.error(new RuntimeException("downstream failure")));

            // Act & Assert
            StepVerifier.create(service.createPortfolioTradingAccount(request))
                .expectErrorMatches(e -> e instanceof RuntimeException
                    && "downstream failure".equals(e.getMessage()))
                .verify();
        }
    }

    // =========================================================================
    // upsertInvestmentPortfolios
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#upsertInvestmentPortfolios(InvestmentArrangement, Map)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Existing portfolio found → patch succeeds</li>
     *   <li>No existing portfolio → create new</li>
     *   <li>Patch fails with {@link WebClientResponseException} → falls back to existing</li>
     *   <li>Multiple portfolios returned → {@link IllegalStateException}</li>
     *   <li>Null arrangement → {@link NullPointerException}</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertInvestmentPortfolios")
    class UpsertInvestmentPortfoliosTests {

        @Test
        @DisplayName("existing portfolio found — patches and returns updated portfolio")
        void upsertInvestmentPortfolios_existingPortfolio_patchesAndReturns() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            String externalId = "PORTFOLIO-EXT-PATCH";
            String leExternalId = "LE-001";
            UUID clientUuid = UUID.randomUUID();

            InvestmentArrangement arrangement = buildArrangement(externalId, "Test Portfolio", productId, leExternalId);
            PortfolioList existing = buildPortfolioList(portfolioUuid, externalId, OffsetDateTime.now().minusMonths(6));
            PortfolioList patched = buildPortfolioList(portfolioUuid, externalId, OffsetDateTime.now().minusMonths(6));
            PortfolioList fallbackCreated = buildPortfolioList(UUID.randomUUID(), externalId, OffsetDateTime.now().minusMonths(6));

            PaginatedPortfolioListList paginatedList = Mockito.mock(PaginatedPortfolioListList.class);
            when(paginatedList.getResults()).thenReturn(List.of(existing));
            when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
                isNull(), eq(externalId), isNull(), isNull(), eq(1),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(paginatedList));
            when(portfolioApi.createPortfolio(any(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(fallbackCreated));
            when(portfolioApi.patchPortfolio(eq(portfolioUuid.toString()), isNull(), isNull(), isNull(), any()))
                .thenReturn(Mono.just(patched));

            Map<String, List<UUID>> clientsByLeExternalId = Map.of(leExternalId, List.of(clientUuid));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentPortfolios(arrangement, clientsByLeExternalId))
                .expectNextMatches(p -> portfolioUuid.equals(p.getUuid()))
                .verifyComplete();

            verify(portfolioApi).patchPortfolio(eq(portfolioUuid.toString()), isNull(), isNull(), isNull(), any());
            verify(portfolioApi, never()).createPortfolio(any(), any(), any(), any());
        }

        @Test
        @DisplayName("no existing portfolio — creates and returns new portfolio")
        void upsertInvestmentPortfolios_noExistingPortfolio_createsNew() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            String externalId = "PORTFOLIO-EXT-NEW";
            String leExternalId = "LE-002";
            UUID clientUuid = UUID.randomUUID();

            InvestmentArrangement arrangement = buildArrangement(externalId, "New Portfolio", productId, leExternalId);

            PaginatedPortfolioListList emptyList = Mockito.mock(PaginatedPortfolioListList.class);
            when(emptyList.getResults()).thenReturn(List.of());
            when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
                isNull(), eq(externalId), isNull(), isNull(), eq(1),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(emptyList));

            PortfolioList created = buildPortfolioList(portfolioUuid, externalId, OffsetDateTime.now().minusMonths(6));
            when(portfolioApi.createPortfolio(any(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            Map<String, List<UUID>> clientsByLeExternalId = Map.of(leExternalId, List.of(clientUuid));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentPortfolios(arrangement, clientsByLeExternalId))
                .expectNextMatches(p -> portfolioUuid.equals(p.getUuid()))
                .verifyComplete();

            verify(portfolioApi).createPortfolio(any(), isNull(), isNull(), isNull());
            verify(portfolioApi, never()).patchPortfolio(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("patch fails with WebClientResponseException — falls back to existing portfolio")
        void upsertInvestmentPortfolios_patchFails_withWebClientException_fallsBackToExisting() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            String externalId = "PORTFOLIO-EXT-PATCH-FAIL";
            String leExternalId = "LE-003";
            UUID clientUuid = UUID.randomUUID();

            InvestmentArrangement arrangement = buildArrangement(externalId, "Patch Fail Portfolio", productId, leExternalId);
            PortfolioList existing = buildPortfolioList(portfolioUuid, externalId, OffsetDateTime.now().minusMonths(6));

            PaginatedPortfolioListList paginatedList = Mockito.mock(PaginatedPortfolioListList.class);
            when(paginatedList.getResults()).thenReturn(List.of(existing));
            when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
                isNull(), eq(externalId), isNull(), isNull(), eq(1),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(paginatedList));

            when(portfolioApi.patchPortfolio(eq(portfolioUuid.toString()), isNull(), isNull(), isNull(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity",
                    HttpHeaders.EMPTY, "patch error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

            Map<String, List<UUID>> clientsByLeExternalId = Map.of(leExternalId, List.of(clientUuid));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentPortfolios(arrangement, clientsByLeExternalId))
                .expectNextMatches(p -> portfolioUuid.equals(p.getUuid()))
                .verifyComplete();

            verify(portfolioApi, never()).createPortfolio(any(), any(), any(), any());
        }

        @Test
        @DisplayName("multiple portfolios returned for same externalId — returns IllegalStateException")
        void upsertInvestmentPortfolios_multipleExistingPortfolios_returnsError() {
            // Arrange
            UUID productId = UUID.randomUUID();
            String externalId = "PORTFOLIO-EXT-DUP";
            String leExternalId = "LE-DUP";

            InvestmentArrangement arrangement = buildArrangement(externalId, "Dup Portfolio", productId, leExternalId);

            PortfolioList p1 = buildPortfolioList(UUID.randomUUID(), externalId, OffsetDateTime.now().minusMonths(6));
            PortfolioList p2 = buildPortfolioList(UUID.randomUUID(), externalId, OffsetDateTime.now().minusMonths(6));

            PaginatedPortfolioListList paginatedList = Mockito.mock(PaginatedPortfolioListList.class);
            when(paginatedList.getResults()).thenReturn(List.of(p1, p2));
            when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
                isNull(), eq(externalId), isNull(), isNull(), eq(1),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(paginatedList));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentPortfolios(arrangement, Map.of()))
                .expectErrorMatches(IllegalStateException.class::isInstance)
                .verify();
        }

        @Test
        @DisplayName("null arrangement — throws NullPointerException immediately")
        void upsertInvestmentPortfolios_nullArrangement_throwsNullPointerException() {
            StepVerifier.create(service.upsertInvestmentPortfolios(null, Map.of()))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    // =========================================================================
    // upsertPortfolios (batch)
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#upsertPortfolios(List, Map)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Multiple arrangements → all portfolios returned</li>
     *   <li>Empty arrangements list → empty list returned</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertPortfolios")
    class UpsertPortfoliosTests {

        @Test
        @DisplayName("multiple arrangements — all portfolios created and returned")
        void upsertPortfolios_multipleArrangements_returnsAllPortfolios() {
            // Arrange
            UUID portfolioUuid1 = UUID.randomUUID();
            UUID portfolioUuid2 = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            String externalId1 = "EXT-BATCH-001";
            String externalId2 = "EXT-BATCH-002";
            String leExternalId = "LE-BATCH";
            UUID clientUuid = UUID.randomUUID();

            InvestmentArrangement arrangement1 = buildArrangement(externalId1, "Portfolio 1", productId, leExternalId);
            InvestmentArrangement arrangement2 = buildArrangement(externalId2, "Portfolio 2", productId, leExternalId);

            // Stub listPortfolios per externalId — both return empty (no existing portfolio)
            PaginatedPortfolioListList emptyList1 = Mockito.mock(PaginatedPortfolioListList.class);
            when(emptyList1.getResults()).thenReturn(List.of());
            when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
                isNull(), eq(externalId1), isNull(), isNull(), eq(1),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(emptyList1));

            PaginatedPortfolioListList emptyList2 = Mockito.mock(PaginatedPortfolioListList.class);
            when(emptyList2.getResults()).thenReturn(List.of());
            when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
                isNull(), eq(externalId2), isNull(), isNull(), eq(1),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(emptyList2));

            // createPortfolio is called once per arrangement — chain returns so each call gets the right result
            PortfolioList created1 = buildPortfolioList(portfolioUuid1, externalId1, OffsetDateTime.now().minusMonths(6));
            PortfolioList created2 = buildPortfolioList(portfolioUuid2, externalId2, OffsetDateTime.now().minusMonths(6));
            when(portfolioApi.createPortfolio(any(), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(created1))
                .thenReturn(Mono.just(created2));

            Map<String, List<UUID>> clientsByLeExternalId = Map.of(leExternalId, List.of(clientUuid));

            // Act & Assert
            StepVerifier.create(service.upsertPortfolios(List.of(arrangement1, arrangement2), clientsByLeExternalId))
                .expectNextMatches(list -> list.size() == 2)
                .verifyComplete();
        }

        @Test
        @DisplayName("empty arrangements list — returns empty list without calling API")
        void upsertPortfolios_emptyArrangements_returnsEmptyList() {
            StepVerifier.create(service.upsertPortfolios(List.of(), Map.of()))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

            verifyNoInteractions(portfolioApi);
        }
    }

    // =========================================================================
    // upsertDeposits
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#upsertDeposits(PortfolioList)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>No existing deposits → creates default deposit of 10,000</li>
     *   <li>Existing deposits sum less than default → creates top-up deposit</li>
     *   <li>Existing deposits sum equals or exceeds default → returns last deposit without creating</li>
     *   <li>Null deposit results → creates default deposit</li>
     *   <li>API error listing deposits → returns fallback deposit</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertDeposits")
    class UpsertDepositsTests {

        @Test
        @DisplayName("no existing deposits — creates default deposit of 10,000")
        void upsertDeposits_noExistingDeposits_createsDefaultDeposit() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid, "EXT-DEP-001",
                OffsetDateTime.now().minusMonths(6));

            when(paymentsApi.listDeposits(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(portfolioUuid), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedDepositList().results(List.of())));

            Deposit created = Mockito.mock(Deposit.class);
            when(created.getAmount()).thenReturn(10_000d);
            when(paymentsApi.createDeposit(any(DepositRequest.class)))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertDeposits(portfolio))
                .expectNextMatches(d -> Double.valueOf(10_000d).equals(d.getAmount()))
                .verifyComplete();

            verify(paymentsApi).createDeposit(any(DepositRequest.class));
        }

        @Test
        @DisplayName("existing deposits sum less than default — creates top-up deposit for remaining amount")
        void upsertDeposits_existingDepositsLessThanDefault_topsUpRemainingAmount() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid, "EXT-DEP-002",
                OffsetDateTime.now().minusMonths(6));

            Deposit existingDeposit = Mockito.mock(Deposit.class);
            when(existingDeposit.getAmount()).thenReturn(4_000d);

            when(paymentsApi.listDeposits(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(portfolioUuid), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedDepositList().results(List.of(existingDeposit))));

            Deposit topUpDeposit = Mockito.mock(Deposit.class);
            when(topUpDeposit.getAmount()).thenReturn(6_000d);
            when(paymentsApi.createDeposit(any(DepositRequest.class)))
                .thenReturn(Mono.just(topUpDeposit));

            // Act & Assert
            StepVerifier.create(service.upsertDeposits(portfolio))
                .expectNextMatches(d -> Double.valueOf(6_000d).equals(d.getAmount()))
                .verifyComplete();

            verify(paymentsApi).createDeposit(argThat(req -> req.getAmount() == 6_000d));
        }

        @Test
        @DisplayName("existing deposits equal to default — returns last deposit without creating a new one")
        void upsertDeposits_existingDepositsEqualToDefault_doesNotCreateNewDeposit() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid, "EXT-DEP-003",
                OffsetDateTime.now().minusMonths(6));

            Deposit existingDeposit = Mockito.mock(Deposit.class);
            when(existingDeposit.getAmount()).thenReturn(10_000d);

            when(paymentsApi.listDeposits(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(portfolioUuid), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedDepositList().results(List.of(existingDeposit))));

            Deposit fallbackDeposit = Mockito.mock(Deposit.class);
            when(paymentsApi.createDeposit(any())).thenReturn(Mono.just(fallbackDeposit));
            // Act & Assert
            StepVerifier.create(service.upsertDeposits(portfolio))
                .expectNextMatches(d -> Double.valueOf(10_000d).equals(d.getAmount()))
                .verifyComplete();

            verify(paymentsApi, never()).createDeposit(any());
        }

        @Test
        @DisplayName("null deposit results in paginated response — creates default deposit")
        void upsertDeposits_nullDepositResultList_createsDefaultDeposit() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            PortfolioList portfolio = buildPortfolioList(portfolioUuid, "EXT-DEP-NULL",
                OffsetDateTime.now().minusMonths(6));

            when(paymentsApi.listDeposits(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(portfolioUuid), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(new PaginatedDepositList().results(null)));

            Deposit created = Mockito.mock(Deposit.class);
            when(created.getAmount()).thenReturn(10_000d);
            when(paymentsApi.createDeposit(any(DepositRequest.class)))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertDeposits(portfolio))
                .expectNextMatches(d -> Double.valueOf(10_000d).equals(d.getAmount()))
                .verifyComplete();

            verify(paymentsApi).createDeposit(any(DepositRequest.class));
        }

        @Test
        @DisplayName("API error listing deposits — returns fallback deposit with portfolio UUID and default amount")
        void upsertDeposits_apiError_returnsFallbackDeposit() {
            // Arrange
            UUID portfolioUuid = UUID.randomUUID();
            OffsetDateTime activated = OffsetDateTime.now().minusMonths(6);
            PortfolioList portfolio = buildPortfolioList(portfolioUuid, "EXT-DEP-ERR", activated);

            when(paymentsApi.listDeposits(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(portfolioUuid), isNull(), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));

            // Act & Assert
            StepVerifier.create(service.upsertDeposits(portfolio))
                .expectNextMatches(d -> portfolioUuid.equals(d.getPortfolio())
                    && d.getAmount() == 10_000d)
                .verifyComplete();

            verify(paymentsApi, never()).createDeposit(any());
        }
    }

    // =========================================================================
    // upsertInvestmentProducts
    // =========================================================================

    /**
     * Tests for {@link InvestmentPortfolioService#upsertInvestmentProducts(InvestmentData, List)}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Unknown product type → {@link IllegalStateException}</li>
     *   <li>SELF_TRADING type, existing product found → patch</li>
     *   <li>SELF_TRADING type, no existing product → create</li>
     *   <li>Non-SELF_TRADING (ROBO) type, model portfolio found → create with model</li>
     *   <li>Non-SELF_TRADING (ROBO) type, no model portfolio → {@link IllegalStateException}</li>
     *   <li>Multiple arrangements with same product type → deduplicated to single product</li>
     *   <li>Patch product fails with {@link WebClientResponseException} → falls back to existing</li>
     *   <li>Null arrangements list → {@link NullPointerException}</li>
     * </ul>
     */
    @Nested
    @DisplayName("upsertInvestmentProducts")
    class UpsertInvestmentProductsTests {

        @Test
        @DisplayName("unknown product type — returns IllegalStateException")
        void upsertInvestmentProducts_unknownProductType_returnsError() {
            // Arrange
            InvestmentArrangement arrangement = buildArrangementWithProductType(
                "ARR-UNKNOWN-TYPE", "Unknown Type Arrangement", "UNKNOWN_TYPE");
            InvestmentData investmentData = Mockito.mock(InvestmentData.class);

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arrangement)))
                .expectErrorMatches(IllegalStateException.class::isInstance)
                .verify();
        }

        @Test
        @DisplayName("SELF_TRADING type with existing product — patches and returns existing product")
        void upsertInvestmentProducts_selfTradingType_existingProduct_patchesAndReturns() {
            // Arrange
            UUID productUuid = UUID.randomUUID();
            String externalId = "ARR-SELF-TRADING-PATCH";
            InvestmentArrangement arrangement = buildArrangementWithProductType(
                externalId, "Self Trading Patch", ProductTypeEnum.SELF_TRADING.getValue());

            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            when(investmentData.getModelPortfolios()).thenReturn(List.of());

            PortfolioProduct existingProduct = buildPortfolioProduct(productUuid, ProductTypeEnum.SELF_TRADING);
            PortfolioProduct patched = buildPortfolioProduct(productUuid, ProductTypeEnum.SELF_TRADING);
            PortfolioProduct fallbackCreated = buildPortfolioProduct(UUID.randomUUID(), ProductTypeEnum.SELF_TRADING);

            PaginatedPortfolioProductList productList = Mockito.mock(PaginatedPortfolioProductList.class);
            when(productList.getResults()).thenReturn(List.of(existingProduct));
            when(productsApi.listPortfolioProducts(any(), isNull(), isNull(),
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), any(),
                eq(List.of(ProductTypeEnum.SELF_TRADING.getValue()))))
                .thenReturn(Mono.just(productList));
            when(productsApi.createPortfolioProduct(any(), any(), isNull(), isNull()))
                .thenReturn(Mono.just(fallbackCreated));
            when(productsApi.patchPortfolioProduct(
                eq(productUuid.toString()), any(), isNull(), isNull(), any()))
                .thenReturn(Mono.just(patched));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arrangement)))
                .expectNextMatches(products -> products.size() == 1
                    && productUuid.equals(products.getFirst().getUuid()))
                .verifyComplete();

            verify(productsApi).patchPortfolioProduct(
                eq(productUuid.toString()), any(), isNull(), isNull(), any());
            verify(productsApi, never()).createPortfolioProduct(any(), any(), any(), any());
        }

        @Test
        @DisplayName("SELF_TRADING type with no existing product — creates new product")
        void upsertInvestmentProducts_selfTradingType_noExistingProduct_createsNew() {
            // Arrange
            UUID newProductUuid = UUID.randomUUID();
            String externalId = "ARR-SELF-TRADING-NEW";
            InvestmentArrangement arrangement = buildArrangementWithProductType(
                externalId, "Self Trading New", ProductTypeEnum.SELF_TRADING.getValue());

            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            when(investmentData.getModelPortfolios()).thenReturn(List.of());

            PaginatedPortfolioProductList emptyList = Mockito.mock(PaginatedPortfolioProductList.class);
            when(emptyList.getResults()).thenReturn(List.of());
            when(productsApi.listPortfolioProducts(any(), isNull(), isNull(),
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), any(),
                eq(List.of(ProductTypeEnum.SELF_TRADING.getValue()))))
                .thenReturn(Mono.just(emptyList));

            PortfolioProduct created = buildPortfolioProduct(newProductUuid, ProductTypeEnum.SELF_TRADING);
            when(productsApi.createPortfolioProduct(any(), any(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arrangement)))
                .expectNextMatches(products -> products.size() == 1
                    && newProductUuid.equals(products.getFirst().getUuid()))
                .verifyComplete();

            verify(productsApi).createPortfolioProduct(any(), any(), isNull(), isNull());
            verify(productsApi, never()).patchPortfolioProduct(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("ROBO type with model portfolio found — creates product linked to model portfolio")
        void upsertInvestmentProducts_roboType_withModelPortfolio_createsProductWithModel() {
            // Arrange
            UUID newProductUuid = UUID.randomUUID();
            UUID modelUuid = UUID.randomUUID();
            String externalId = "ARR-ROBO-001";
            InvestmentArrangement arrangement = buildArrangementWithProductType(
                externalId, "Robo Arrangement", ProductTypeEnum.ROBO_ADVISOR.getValue());

            ModelPortfolio modelPortfolio = Mockito.mock(ModelPortfolio.class);
            when(modelPortfolio.getUuid()).thenReturn(modelUuid);
            when(modelPortfolio.getRiskLevel()).thenReturn(3);
            when(modelPortfolio.getProductTypeEnum()).thenReturn(ProductTypeEnum.ROBO_ADVISOR);

            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            when(investmentData.getModelPortfolios()).thenReturn(List.of(modelPortfolio));

            PaginatedPortfolioProductList emptyList = Mockito.mock(PaginatedPortfolioProductList.class);
            when(emptyList.getResults()).thenReturn(List.of());
            when(productsApi.listPortfolioProducts(any(), isNull(), isNull(),
                eq(1), isNull(), isNull(), eq(3), isNull(), isNull(), any(),
                eq(List.of(ProductTypeEnum.ROBO_ADVISOR.getValue()))))
                .thenReturn(Mono.just(emptyList));

            PortfolioProduct created = buildPortfolioProduct(newProductUuid, ProductTypeEnum.ROBO_ADVISOR);
            when(productsApi.createPortfolioProduct(any(), any(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arrangement)))
                .expectNextMatches(products -> products.size() == 1
                    && newProductUuid.equals(products.getFirst().getUuid()))
                .verifyComplete();

            verify(productsApi).createPortfolioProduct(any(), any(), isNull(), isNull());
        }

        @Test
        @DisplayName("ROBO type with no model portfolio — returns IllegalStateException")
        void upsertInvestmentProducts_roboType_noModelPortfolio_returnsError() {
            // Arrange
            String externalId = "ARR-ROBO-NO-MODEL";
            InvestmentArrangement arrangement = buildArrangementWithProductType(
                externalId, "Robo No Model", ProductTypeEnum.ROBO_ADVISOR.getValue());

            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            when(investmentData.getModelPortfolios()).thenReturn(List.of());

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arrangement)))
                .expectErrorMatches(IllegalStateException.class::isInstance)
                .verify();
        }

        @Test
        @DisplayName("two arrangements with same SELF_TRADING product type — deduplicated to single product")
        void upsertInvestmentProducts_multipleArrangementsWithSameProductType_deduplicatesToOneProduct() {
            // Arrange
            UUID productUuid = UUID.randomUUID();
            InvestmentArrangement arr1 = buildArrangementWithProductType(
                "ARR-DEDUP-001", "Dedup 1", ProductTypeEnum.SELF_TRADING.getValue());
            InvestmentArrangement arr2 = buildArrangementWithProductType(
                "ARR-DEDUP-002", "Dedup 2", ProductTypeEnum.SELF_TRADING.getValue());

            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            when(investmentData.getModelPortfolios()).thenReturn(List.of());

            PaginatedPortfolioProductList emptyList = Mockito.mock(PaginatedPortfolioProductList.class);
            when(emptyList.getResults()).thenReturn(List.of());
            when(productsApi.listPortfolioProducts(any(), isNull(), isNull(),
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), any(),
                eq(List.of(ProductTypeEnum.SELF_TRADING.getValue()))))
                .thenReturn(Mono.just(emptyList));

            PortfolioProduct created = buildPortfolioProduct(productUuid, ProductTypeEnum.SELF_TRADING);
            when(productsApi.createPortfolioProduct(any(), any(), isNull(), isNull()))
                .thenReturn(Mono.just(created));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arr1, arr2)))
                .expectNextMatches(products -> products.size() == 1)
                .verifyComplete();

            verify(productsApi, times(1)).createPortfolioProduct(any(), any(), isNull(), isNull());
        }

        @Test
        @DisplayName("patch product fails with WebClientResponseException — falls back to existing product")
        void upsertInvestmentProducts_patchFails_withWebClientException_fallsBackToExisting() {
            // Arrange
            UUID productUuid = UUID.randomUUID();
            InvestmentArrangement arrangement = buildArrangementWithProductType(
                "ARR-PATCH-FAIL", "Patch Fail", ProductTypeEnum.SELF_TRADING.getValue());

            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            when(investmentData.getModelPortfolios()).thenReturn(List.of());

            PortfolioProduct existingProduct = buildPortfolioProduct(productUuid, ProductTypeEnum.SELF_TRADING);

            PaginatedPortfolioProductList productList = Mockito.mock(PaginatedPortfolioProductList.class);
            when(productList.getResults()).thenReturn(List.of(existingProduct));
            when(productsApi.listPortfolioProducts(any(), isNull(), isNull(),
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), any(),
                eq(List.of(ProductTypeEnum.SELF_TRADING.getValue()))))
                .thenReturn(Mono.just(productList));
            when(productsApi.patchPortfolioProduct(
                eq(productUuid.toString()), any(), isNull(), isNull(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity",
                    HttpHeaders.EMPTY, "patch error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

            // Act & Assert
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, List.of(arrangement)))
                .expectNextMatches(products -> products.size() == 1
                    && productUuid.equals(products.getFirst().getUuid()))
                .verifyComplete();

            verify(productsApi, never()).createPortfolioProduct(any(), any(), any(), any());
        }

        @Test
        @DisplayName("null arrangements list — throws NullPointerException immediately")
        void upsertInvestmentProducts_nullArrangements_throwsNullPointerException() {
            InvestmentData investmentData = Mockito.mock(InvestmentData.class);
            StepVerifier.create(service.upsertInvestmentProducts(investmentData, null))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a mocked {@link PortfolioList} with the given UUID, externalId, and activation date.
     */
    private PortfolioList buildPortfolioList(UUID portfolioUuid, String externalId, OffsetDateTime activated) {
        PortfolioList portfolio = Mockito.mock(PortfolioList.class);
        when(portfolio.getUuid()).thenReturn(portfolioUuid);
        when(portfolio.getExternalId()).thenReturn(externalId);
        when(portfolio.getActivated()).thenReturn(activated);
        when(portfolio.getStatus()).thenReturn(StatusA3dEnum.ACTIVE);
        return portfolio;
    }

    /**
     * Builds a mocked {@link PortfolioProduct} with the given UUID and product type.
     * Model portfolio and advice engine are set to null for SELF_TRADING; callers
     * should override these stubs for non-SELF_TRADING types.
     */
    private PortfolioProduct buildPortfolioProduct(UUID uuid, ProductTypeEnum productType) {
        PortfolioProduct product = Mockito.mock(PortfolioProduct.class);
        when(product.getUuid()).thenReturn(uuid);
        when(product.getProductType()).thenReturn(productType);
        when(product.getAdviceEngine()).thenReturn(null);
        when(product.getModelPortfolio()).thenReturn(null);
        when(product.getExtraData()).thenReturn(null);
        return product;
    }

    /**
     * Builds a mocked {@link InvestmentArrangement} with product ID and legal entity external ID.
     * Used for portfolio upsert tests.
     */
    private InvestmentArrangement buildArrangement(String externalId, String name,
        UUID productId, String legalEntityExternalId) {
        InvestmentArrangement arrangement = Mockito.mock(InvestmentArrangement.class);
        when(arrangement.getExternalId()).thenReturn(externalId);
        when(arrangement.getName()).thenReturn(name);
        when(arrangement.getInvestmentProductId()).thenReturn(productId);
        when(arrangement.getLegalEntityIds()).thenReturn(List.of(legalEntityExternalId));
        when(arrangement.getCurrency()).thenReturn("EUR");
        when(arrangement.getInternalId()).thenReturn(null);
        return arrangement;
    }

    /**
     * Builds a mocked {@link InvestmentArrangement} with a product type external ID.
     * Used for investment product upsert tests.
     */
    private InvestmentArrangement buildArrangementWithProductType(String externalId, String name,
        String productTypeValue) {
        InvestmentArrangement arrangement = Mockito.mock(InvestmentArrangement.class);
        when(arrangement.getExternalId()).thenReturn(externalId);
        when(arrangement.getName()).thenReturn(name);
        when(arrangement.getProductTypeExternalId()).thenReturn(productTypeValue);
        when(arrangement.getLegalEntityIds()).thenReturn(List.of());
        when(arrangement.getCurrency()).thenReturn("EUR");
        when(arrangement.getInternalId()).thenReturn(null);
        return arrangement;
    }


    /**
     * Stubs {@link PortfolioApi#listPortfolios} to return an existing portfolio with the given UUID.
     * Used in trading account tests that need a resolved portfolio UUID.
     */
    private void mockPortfolioFound(String externalId, UUID portfolioUuid) {
        PortfolioList portfolioList = buildPortfolioList(portfolioUuid, externalId,
            OffsetDateTime.now().minusMonths(6));
        PaginatedPortfolioListList paginatedList = Mockito.mock(PaginatedPortfolioListList.class);
        when(paginatedList.getResults()).thenReturn(List.of(portfolioList));

        when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
            isNull(), eq(externalId), isNull(), isNull(), eq(1),
            isNull(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(paginatedList));
    }
}
