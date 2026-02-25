package com.backbase.stream.investment.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PaymentsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.PortfolioTradingAccountsApi;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioListList;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioTradingAccountList;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioTradingAccount;
import com.backbase.investment.api.service.v1.model.PortfolioTradingAccountRequest;
import com.backbase.investment.api.service.v1.model.StatusA3dEnum;
import com.backbase.stream.configuration.InvestmentIngestionConfigurationProperties;
import com.backbase.stream.investment.model.InvestmentPortfolioTradingAccount;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class InvestmentPortfolioServiceTest {

    private InvestmentProductsApi productsApi;
    private PortfolioApi portfolioApi;
    private PaymentsApi paymentsApi;
    private PortfolioTradingAccountsApi portfolioTradingAccountsApi;
    private InvestmentIngestionConfigurationProperties config;
    private InvestmentPortfolioService service;

    @BeforeEach
    void setUp() {
        productsApi = Mockito.mock(InvestmentProductsApi.class);
        portfolioApi = Mockito.mock(PortfolioApi.class);
        paymentsApi = Mockito.mock(PaymentsApi.class);
        portfolioTradingAccountsApi = Mockito.mock(PortfolioTradingAccountsApi.class);
        config = Mockito.mock(InvestmentIngestionConfigurationProperties.class);
        when(config.getPortfolioActivationPastMonths()).thenReturn(6);
        service = new InvestmentPortfolioService(
            productsApi, portfolioApi, paymentsApi, portfolioTradingAccountsApi, config);
    }

    // -----------------------------------------------------------------------
    // upsertPortfolioTradingAccount — patch path
    // -----------------------------------------------------------------------

    @Test
    void upsertPortfolioTradingAccount_existingAccount_patchesAndReturns() {
        UUID existingUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioTradingAccount existing = Mockito.mock(PortfolioTradingAccount.class);
        when(existing.getUuid()).thenReturn(existingUuid);
        when(existing.getExternalAccountId()).thenReturn("EXT-001");

        PortfolioTradingAccount patched = Mockito.mock(PortfolioTradingAccount.class);
        when(patched.getUuid()).thenReturn(existingUuid);
        when(patched.getExternalAccountId()).thenReturn("EXT-001");

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-001")
            .accountId("ACC-001")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        PaginatedPortfolioTradingAccountList accountList = new PaginatedPortfolioTradingAccountList()
            .results(List.of(existing));

        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-001"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(accountList));

        when(portfolioTradingAccountsApi.patchPortfolioTradingAccount(
            eq(existingUuid.toString()), any()))
            .thenReturn(Mono.just(patched));

        StepVerifier.create(service.upsertPortfolioTradingAccount(request))
            .expectNextMatches(a -> a.getUuid().equals(existingUuid))
            .verifyComplete();
    }

    @Test
    void upsertPortfolioTradingAccount_noExistingAccount_createsNew() {
        UUID newUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioTradingAccount created = Mockito.mock(PortfolioTradingAccount.class);
        when(created.getUuid()).thenReturn(newUuid);
        when(created.getExternalAccountId()).thenReturn("EXT-002");

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-002")
            .accountId("ACC-002")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-002"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList().results(List.of())));

        when(portfolioTradingAccountsApi.createPortfolioTradingAccount((request)))
            .thenReturn(Mono.just(created));

        StepVerifier.create(service.upsertPortfolioTradingAccount(request))
            .expectNextMatches(a -> a.getUuid().equals(newUuid))
            .verifyComplete();
    }

    @Test
    void upsertPortfolioTradingAccount_patchFails_withWebClientException_fallsBackToExisting() {
        UUID existingUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioTradingAccount existing = Mockito.mock(PortfolioTradingAccount.class);
        when(existing.getUuid()).thenReturn(existingUuid);
        when(existing.getExternalAccountId()).thenReturn("EXT-003");

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-003")
            .accountId("ACC-003")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        PaginatedPortfolioTradingAccountList accountList = new PaginatedPortfolioTradingAccountList()
            .results(List.of(existing));

        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-003"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(accountList));

        when(portfolioTradingAccountsApi.patchPortfolioTradingAccount(
            (existingUuid.toString()), (request)))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                HttpHeaders.EMPTY, null, StandardCharsets.UTF_8)));

        when(portfolioTradingAccountsApi.createPortfolioTradingAccount(any()))
            .thenReturn(Mono.just(existing));

        StepVerifier.create(service.upsertPortfolioTradingAccount(request))
            .expectNextMatches(a -> a.getUuid().equals(existingUuid))
            .verifyComplete();
    }

    @Test
    void upsertPortfolioTradingAccount_patchFails_withNonWebClientException_propagatesError() {
        UUID existingUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioTradingAccount existing = Mockito.mock(PortfolioTradingAccount.class);
        when(existing.getUuid()).thenReturn(existingUuid);
        when(existing.getExternalAccountId()).thenReturn("EXT-004");

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-004")
            .accountId("ACC-004")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        PaginatedPortfolioTradingAccountList accountList = new PaginatedPortfolioTradingAccountList()
            .results(List.of(existing));

        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-004"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(accountList));

        when(portfolioTradingAccountsApi.patchPortfolioTradingAccount(
            (existingUuid.toString()), (request)))
            .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        StepVerifier.create(service.upsertPortfolioTradingAccount(request))
            .expectErrorMatches(e -> e instanceof RuntimeException
                && e.getMessage().equals("Unexpected error"))
            .verify();
    }

    @Test
    void upsertPortfolioTradingAccount_multipleExistingAccounts_returnsError() {
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioTradingAccount acc1 = Mockito.mock(PortfolioTradingAccount.class);
        when(acc1.getUuid()).thenReturn(UUID.randomUUID());
        PortfolioTradingAccount acc2 = Mockito.mock(PortfolioTradingAccount.class);
        when(acc2.getUuid()).thenReturn(UUID.randomUUID());

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-DUP")
            .accountId("ACC-DUP")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-DUP"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList()
                .results(List.of(acc1, acc2))));

        // Defensive stub — prevents NPE if switchIfEmpty is accidentally reached
        when(portfolioTradingAccountsApi.createPortfolioTradingAccount(any()))
            .thenReturn(Mono.error(new IllegalStateException("should not be called")));

        StepVerifier.create(service.upsertPortfolioTradingAccount(request))
            .expectErrorMatches(e -> e instanceof IllegalStateException
                && e.getMessage().contains("Found 2 portfolio trading accounts"))
            .verify();
    }

    // -----------------------------------------------------------------------
    // upsertPortfolioTradingAccounts — batch resilience
    // -----------------------------------------------------------------------

    @Test
    void upsertPortfolioTradingAccounts_nullInput_returnsEmptyList() {
        StepVerifier.create(service.upsertPortfolioTradingAccounts(null))
            .expectNext(List.of())
            .verifyComplete();
    }

    @Test
    void upsertPortfolioTradingAccounts_emptyInput_returnsEmptyList() {
        StepVerifier.create(service.upsertPortfolioTradingAccounts(List.of()))
            .expectNext(List.of())
            .verifyComplete();
    }

    @Test
    void upsertPortfolioTradingAccounts_singleFailure_doesNotStopBatch() {
        UUID portfolioUuid1 = UUID.randomUUID();
        UUID portfolioUuid2 = UUID.randomUUID();
        String externalId1 = "PORTFOLIO-EXT-001";
        String externalId2 = "PORTFOLIO-EXT-002";

        // Mock portfolio lookups — uses externalId, not uuid setter
        mockPortfolioFound(externalId1, portfolioUuid1);
        mockPortfolioFound(externalId2, portfolioUuid2);

        // Account 1: list returns empty → create fails
        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-FAIL"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList().results(List.of())));
        when(portfolioTradingAccountsApi.createPortfolioTradingAccount(
            argThat(r -> r != null && "EXT-FAIL".equals(r.getExternalAccountId()))))
            .thenReturn(Mono.error(new RuntimeException("Create failed")));

        // Account 2: list returns empty → create succeeds
        UUID createdUuid = UUID.randomUUID();
        PortfolioTradingAccount created = Mockito.mock(PortfolioTradingAccount.class);
        when(created.getUuid()).thenReturn(createdUuid);
        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-OK"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(new PaginatedPortfolioTradingAccountList().results(List.of())));
        when(portfolioTradingAccountsApi.createPortfolioTradingAccount(
            argThat(r -> r != null && "EXT-OK".equals(r.getExternalAccountId()))))
            .thenReturn(Mono.just(created));

        List<InvestmentPortfolioTradingAccount> input = List.of(
            buildTradingAccountInput("EXT-FAIL", externalId1),
            buildTradingAccountInput("EXT-OK", externalId2)
        );

        StepVerifier.create(service.upsertPortfolioTradingAccounts(input))
            .expectNextMatches(list -> list.size() == 1 && list.get(0).getUuid().equals(createdUuid))
            .verifyComplete();
    }

    @Test
    void upsertPortfolioTradingAccounts_allFail_returnsEmptyList() {
        String externalId = "PORTFOLIO-EXT-ALL-FAIL";
        UUID portfolioUuid = UUID.randomUUID();

        mockPortfolioFound(externalId, portfolioUuid);

        when(portfolioTradingAccountsApi.listPortfolioTradingAccounts(
            eq(1), isNull(), isNull(), eq("EXT-ALL-FAIL"), isNull(), isNull(), isNull()))
            .thenReturn(Mono.error(new RuntimeException("List failed")));

        List<InvestmentPortfolioTradingAccount> input = List.of(
            buildTradingAccountInput("EXT-ALL-FAIL", externalId)
        );

        StepVerifier.create(service.upsertPortfolioTradingAccounts(input))
            .expectNextMatches(List::isEmpty)
            .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // createPortfolioTradingAccount — direct creation
    // -----------------------------------------------------------------------

    @Test
    void createPortfolioTradingAccount_success_returnsCreatedAccount() {
        UUID portfolioUuid = UUID.randomUUID();
        UUID newUuid = UUID.randomUUID();

        PortfolioTradingAccount created = Mockito.mock(PortfolioTradingAccount.class);
        when(created.getUuid()).thenReturn(newUuid);
        when(created.getExternalAccountId()).thenReturn("EXT-NEW");

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-NEW")
            .accountId("ACC-NEW")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        when(portfolioTradingAccountsApi.createPortfolioTradingAccount((request)))
            .thenReturn(Mono.just(created));

        StepVerifier.create(service.createPortfolioTradingAccount(request))
            .expectNextMatches(a -> a.getUuid().equals(newUuid))
            .verifyComplete();
    }

    @Test
    void createPortfolioTradingAccount_apiFails_propagatesError() {
        UUID portfolioUuid = UUID.randomUUID();

        PortfolioTradingAccountRequest request = new PortfolioTradingAccountRequest()
            .externalAccountId("EXT-ERR")
            .accountId("ACC-ERR")
            .portfolio(portfolioUuid)
            .isDefault(false)
            .isInternal(false);

        when(portfolioTradingAccountsApi.createPortfolioTradingAccount((request)))
            .thenReturn(Mono.error(new RuntimeException("Creation failed")));

        StepVerifier.create(service.createPortfolioTradingAccount(request))
            .expectErrorMatches(e -> e instanceof RuntimeException
                && e.getMessage().equals("Creation failed"))
            .verify();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PortfolioList buildPortfolioList(UUID portfolioUuid, String externalId, OffsetDateTime activated) {
        PortfolioList portfolio = Mockito.mock(PortfolioList.class);
        when(portfolio.getUuid()).thenReturn(portfolioUuid);
        when(portfolio.getExternalId()).thenReturn(externalId);
        when(portfolio.getActivated()).thenReturn(activated);
        when(portfolio.getStatus()).thenReturn(StatusA3dEnum.ACTIVE);
        return portfolio;
    }

    private void mockPortfolioFound(String externalId, UUID portfolioUuid) {
        PortfolioList portfolioList = buildPortfolioList(portfolioUuid, externalId, OffsetDateTime.now().minusMonths(6));
        PaginatedPortfolioListList paginatedList = Mockito.mock(PaginatedPortfolioListList.class);
        when(paginatedList.getResults()).thenReturn(List.of(portfolioList));

        when(portfolioApi.listPortfolios(isNull(), isNull(), isNull(),
            isNull(), eq(externalId), isNull(), isNull(), eq(1),
            isNull(), isNull(), isNull(), isNull()))
            .thenReturn(Mono.just(paginatedList));
    }

    private InvestmentPortfolioTradingAccount buildTradingAccountInput(String externalAccountId,
        String portfolioExternalId) {
        return InvestmentPortfolioTradingAccount.builder()
            .accountExternalId(externalAccountId)
            .portfolioExternalId(portfolioExternalId)
            .accountId("ACC-" + externalAccountId)
            .isDefault(false)
            .isInternal(false)
            .build();
    }
}