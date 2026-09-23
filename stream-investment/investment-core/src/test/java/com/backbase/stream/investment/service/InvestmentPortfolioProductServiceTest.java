package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.ModelPortfolio;
import com.backbase.stream.investment.ProductPortfolio;
import com.backbase.stream.investment.service.resttemplate.InvestmentRestProductPortfolioService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit test suite for {@link InvestmentPortfolioProductService}.
 */
class InvestmentPortfolioProductServiceTest {

    private static final String ALLOCATION_ASSET_EXPAND = "model_portfolio.allocation.asset";
    private static final String ORDERING = "-model_portfolio__risk_level";
    private static final int LIST_PRODUCT_PAGE_SIZE = 50;

    @Mock
    private InvestmentProductsApi productsApi;

    @Mock
    private InvestmentModelPortfolioService modelPortfolioService;

    @Mock
    private InvestmentRestProductPortfolioService investmentRestProductPortfolioService;

    @Mock
    private InvestmentPortfolioProductDocumentService investmentPortfolioProductDocumentService;

    private final IngestConfigProperties ingestConfigProperties = new IngestConfigProperties();

    private InvestmentPortfolioProductService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InvestmentPortfolioProductService(
            productsApi,
            ingestConfigProperties,
            modelPortfolioService,
            investmentRestProductPortfolioService,
            investmentPortfolioProductDocumentService);
        when(investmentPortfolioProductDocumentService.linkProductDocuments(any(), any()))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    static Stream<Arguments> createBadRequestNotDuplicateExternalIdCases() {
        return Stream.of(
            Arguments.of("without duplicate externalId message", "{\"code\":\"INVALID_INPUT\"}"),
            Arguments.of("missing external_id token", "{\"errors\":{\"name\":[\"already exists\"]}}"),
            Arguments.of("missing already exists token", "{\"errors\":{\"external_id\":[\"invalid value\"]}}"),
            Arguments.of("null response body", null)
        );
    }

    @Nested
    @DisplayName("upsertInvestmentProducts")
    class UpsertInvestmentProductsTests {

        @Test
        @DisplayName("null arrangements — emits NullPointerException")
        void nullArrangements_emitsNullPointerException() {
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of()).build();

            StepVerifier.create(service.upsertInvestmentProducts(data, null))
                .expectError(NullPointerException.class)
                .verify();

            verify(productsApi, never()).listPortfolioProducts(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("no portfolio product templates — returns empty list")
        void noPortfolioProducts_returnsEmptyList() {
            InvestmentData data = InvestmentData.builder().portfolioProducts(null).build();
            InvestmentArrangement arrangement = buildArrangement("self-trading", null);

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();

            verify(investmentRestProductPortfolioService, never()).createPortfolioProduct(any(), any());
            assertThat(arrangement.getInvestmentProductId()).isNull();
        }

        @Test
        @DisplayName("no existing product — creates via REST service and assigns to arrangement")
        void noExistingProduct_createsAndAssigns() {
            UUID createdUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), "Self Trading");

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            PortfolioProduct created = buildApiProduct(createdUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1);
            when(investmentRestProductPortfolioService.createPortfolioProduct(
                any(ProductPortfolio.class), eq(List.of(ALLOCATION_ASSET_EXPAND))))
                .thenReturn(Mono.just(created));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> {
                    assertThat(products).hasSize(1);
                    assertThat(products.getFirst().getUuid()).isEqualTo(createdUuid);
                })
                .verifyComplete();

            assertThat(arrangement.getInvestmentProductId()).isEqualTo(createdUuid);
            assertThat(data.getIngestedPortfolioProducts()).containsExactly(created);
            verify(investmentRestProductPortfolioService, never()).updatePortfolioProduct(any(), any(), any());
        }

        @Test
        @DisplayName("existing product found — patches via REST service")
        void existingProduct_patches() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), "Self Trading");

            PortfolioProduct existing = buildApiProduct(existingUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1);
            stubListReturnsProducts(ProductTypeEnum.SELF_TRADING, existing);

            PortfolioProduct patched = buildApiProduct(existingUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1);
            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(existingUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(investmentRestProductPortfolioService, never()).createPortfolioProduct(any(), any());
            assertThat(arrangement.getInvestmentProductId()).isEqualTo(existingUuid);
        }

        @Test
        @DisplayName("multiple matches — uses last result for patch")
        void multipleMatches_usesLastResult() {
            UUID firstUuid = UUID.randomUUID();
            UUID lastUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Robo Plan", ProductTypeEnum.ROBO_ADVISOR);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Robo Plan");

            PortfolioProduct first = buildApiProduct(firstUuid, "Robo Plan", ProductTypeEnum.ROBO_ADVISOR, 1);
            PortfolioProduct last = buildApiProduct(lastUuid, "Robo Plan", ProductTypeEnum.ROBO_ADVISOR, 2);
            stubListReturnsProducts(ProductTypeEnum.ROBO_ADVISOR, first, last);

            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(lastUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(last));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(lastUuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("duplicate templates by name — processes only one product")
        void duplicateTemplatesByName_processesOnce() {
            UUID productUuid = UUID.randomUUID();
            ProductPortfolio first = buildTemplate("Dedup Product", ProductTypeEnum.SELF_TRADING);
            ProductPortfolio second = buildTemplate("Dedup Product", ProductTypeEnum.SELF_TRADING);
            second.setDescription("replacement");
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(first, second)).build();

            InvestmentArrangement arr1 = buildArrangement(ProductTypeEnum.SELF_TRADING.getValue(), null);
            InvestmentArrangement arr2 = buildArrangement(ProductTypeEnum.SELF_TRADING.getValue(), null);

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            PortfolioProduct created = buildApiProduct(productUuid, "Dedup Product", ProductTypeEnum.SELF_TRADING, 1);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.just(created));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arr1, arr2)))
                .assertNext(products -> assertThat(products).hasSize(1))
                .verifyComplete();

            verify(investmentRestProductPortfolioService, times(1)).createPortfolioProduct(any(), any());
            assertThat(arr1.getInvestmentProductId()).isEqualTo(productUuid);
            assertThat(arr2.getInvestmentProductId()).isEqualTo(productUuid);
        }

        @Test
        @DisplayName("arrangement with productPortfolioName — matches product by name")
        void arrangementWithPortfolioName_matchesByName() {
            UUID matchingUuid = UUID.randomUUID();
            UUID otherUuid = UUID.randomUUID();
            ProductPortfolio targetTemplate = buildTemplate("Target Product", ProductTypeEnum.SELF_TRADING);
            ProductPortfolio otherTemplate = buildTemplate("Other Product", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder()
                .portfolioProducts(List.of(targetTemplate, otherTemplate))
                .build();

            InvestmentArrangement arrangement = InvestmentArrangement.builder()
                .name("Arrangement")
                .productTypeExternalId(ProductTypeEnum.SELF_TRADING.getValue())
                .productPortfolioName("Target Product")
                .build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            PortfolioProduct matching = buildApiProduct(matchingUuid, "Target Product", ProductTypeEnum.SELF_TRADING, 2);
            PortfolioProduct other = buildApiProduct(otherUuid, "Other Product", ProductTypeEnum.SELF_TRADING, 1);

            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.just(matching), Mono.just(other));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products).hasSize(2))
                .verifyComplete();

            assertThat(arrangement.getInvestmentProductId()).isEqualTo(matchingUuid);
        }

        @Test
        @DisplayName("template with model portfolio — upserts model before product create")
        void templateWithModelPortfolio_upsertsModelFirst() {
            UUID modelUuid = UUID.randomUUID();
            UUID productUuid = UUID.randomUUID();
            InvestorModelPortfolio investorModel = new InvestorModelPortfolio(
                null, "Growth Model", 0.25, 7, null, null, null);
            ProductPortfolio template = buildTemplate("Robo Product", ProductTypeEnum.ROBO_ADVISOR);
            template.setModelPortfolio(investorModel);
            template.setProductCategory("retail");

            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Robo Product");

            ModelPortfolio upsertedModel = ModelPortfolio.builder()
                .uuid(modelUuid).name("Growth Model").riskLevel(7).cashWeight(0.25).build();
            when(modelPortfolioService.upsertModelPortfolio(investorModel)).thenReturn(Mono.just(upsertedModel));

            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            PortfolioProduct created = buildApiProduct(productUuid, "Robo Product", ProductTypeEnum.ROBO_ADVISOR, 1);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.just(created));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products).hasSize(1))
                .verifyComplete();

            verify(modelPortfolioService).upsertModelPortfolio(investorModel);
            ArgumentCaptor<ProductPortfolio> templateCaptor = ArgumentCaptor.forClass(ProductPortfolio.class);
            verify(investmentRestProductPortfolioService).createPortfolioProduct(
                templateCaptor.capture(), eq(List.of(ALLOCATION_ASSET_EXPAND)));
            assertThat(templateCaptor.getValue().getModelPortfolio()).isNotNull();
            assertThat(templateCaptor.getValue().getModelPortfolio().getUuid()).isEqualTo(modelUuid);
        }

        @Test
        @DisplayName("patch fails with HttpClientErrorException — falls back to existing product")
        void patchFailsWithHttpClientError_fallsBackToExisting() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), "Self Trading");

            PortfolioProduct existing = buildApiProduct(existingUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1);
            stubListReturnsProducts(ProductTypeEnum.SELF_TRADING, existing);

            HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);
            when(investmentRestProductPortfolioService.updatePortfolioProduct(any(), any(), any()))
                .thenReturn(Mono.error(ex));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> {
                    assertThat(products).hasSize(1);
                    assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid);
                })
                .verifyComplete();

            verify(investmentRestProductPortfolioService, never()).createPortfolioProduct(any(), any());
            assertThat(arrangement.getInvestmentProductId()).isEqualTo(existingUuid);
        }

        @Test
        @DisplayName("list API failure — skips product and completes batch")
        void listApiFailure_skipsProduct() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            when(productsApi.listPortfolioProducts(
                eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(ORDERING),
                eq(List.of(ProductTypeEnum.SELF_TRADING.getValue())), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("list failed")));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), null))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create API failure — skips product and completes batch")
        void createApiFailure_skipsProduct() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(new IllegalStateException("create failed")));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), null))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("one product fails — continues with remaining products")
        void oneProductFails_continuesWithRemaining() {
            UUID successUuid = UUID.randomUUID();
            ProductPortfolio failing = buildTemplate("Failing Product", ProductTypeEnum.SELF_TRADING);
            ProductPortfolio succeeding = buildTemplate("Working Product", ProductTypeEnum.ROBO_ADVISOR);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(failing, succeeding)).build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenAnswer(invocation -> {
                    ProductPortfolio template = invocation.getArgument(0);
                    if ("Failing Product".equals(template.getName())) {
                        return Mono.error(new IllegalStateException("create failed"));
                    }
                    return Mono.just(buildApiProduct(successUuid, "Working Product", ProductTypeEnum.ROBO_ADVISOR, 1));
                });

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(
                buildArrangement(ProductTypeEnum.SELF_TRADING.getValue(), "Failing Product"),
                buildArrangement(ProductTypeEnum.ROBO_ADVISOR.getValue(), "Working Product"))))
                .assertNext(products -> {
                    assertThat(products).hasSize(1);
                    assertThat(products.getFirst().getUuid()).isEqualTo(successUuid);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("existing product found by externalId when name differs — patches without create")
        void existingProduct_foundByExternalIdWhenNameDiffers_patches() {
            UUID existingUuid = UUID.randomUUID();
            String externalId = "ext-portfolio-self-trading-001";
            ProductPortfolio template = buildTemplate("Self-Trading Portfolio", ProductTypeEnum.SELF_TRADING);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), "Self-Trading Portfolio");

            PortfolioProduct existing = buildApiProduct(existingUuid, "Legacy Self Trading Name",
                ProductTypeEnum.SELF_TRADING, 1);
            existing.setExternalId(externalId);
            stubListByExternalIdReturns(externalId, ProductTypeEnum.SELF_TRADING, existing);

            PortfolioProduct patched = buildApiProduct(existingUuid, "Self-Trading Portfolio",
                ProductTypeEnum.SELF_TRADING, 1);
            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(existingUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(patched));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(investmentRestProductPortfolioService, never()).createPortfolioProduct(any(), any());
            verify(productsApi, never()).listPortfolioProducts(
                eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
                any(), any(), any(), any(), any(), eq(ORDERING), any(), any(), any());
            assertThat(arrangement.getInvestmentProductId()).isEqualTo(existingUuid);
        }

        @Test
        @DisplayName("externalId lookup empty — falls back to name search")
        void externalIdLookupEmpty_fallsBackToNameSearch() {
            UUID existingUuid = UUID.randomUUID();
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            PortfolioProduct existing = buildApiProduct(existingUuid, "Balanced", ProductTypeEnum.ROBO_ADVISOR, 13);
            stubListByExternalIdReturnsEmpty(externalId, ProductTypeEnum.ROBO_ADVISOR);
            stubListReturnsProducts(ProductTypeEnum.ROBO_ADVISOR, existing);

            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(existingUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(existing));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();

            verify(investmentRestProductPortfolioService, never()).createPortfolioProduct(any(), any());
        }

        @Test
        @DisplayName("create duplicate externalId (HttpClient) — falls back to patch")
        void createDuplicateExternalId_httpClient_fallsBackToPatch() {
            UUID existingUuid = UUID.randomUUID();
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            PortfolioProduct existing = buildApiProduct(existingUuid, "Balanced", ProductTypeEnum.ROBO_ADVISOR, 13);
            existing.setExternalId(externalId);
            stubListByExternalIdReturnsEmptyThenProduct(externalId, ProductTypeEnum.ROBO_ADVISOR, existing);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);

            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(duplicateExternalIdHttpClientException()));
            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(existingUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(existing));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("create duplicate externalId (WebClient) — falls back to patch")
        void createDuplicateExternalId_webClient_fallsBackToPatch() {
            UUID existingUuid = UUID.randomUUID();
            String externalId = "ext-opportunity-hf-savings-plan-007";
            ProductPortfolio template = buildTemplate("Opportunity Horizon Fund", ProductTypeEnum.SAVINGS_PLAN);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            PortfolioProduct existing = buildApiProduct(
                existingUuid, "Opportunity Horizon Fund", ProductTypeEnum.SAVINGS_PLAN, 7);
            existing.setExternalId(externalId);
            stubListByExternalIdReturnsEmptyThenProduct(externalId, ProductTypeEnum.SAVINGS_PLAN, existing);
            stubListReturnsEmpty(ProductTypeEnum.SAVINGS_PLAN);

            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(duplicateExternalIdWebClientException()));
            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(existingUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(existing));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SAVINGS_PLAN.getValue(), "Opportunity Horizon Fund"))))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("create duplicate externalId but lookup empty — skips product")
        void createDuplicateExternalId_lookupEmpty_skipsProduct() {
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListByExternalIdReturnsEmpty(externalId, ProductTypeEnum.ROBO_ADVISOR);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(duplicateExternalIdHttpClientException()));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create without externalId — duplicate detection not applied")
        void createWithoutExternalId_nonDuplicateError_skipsProduct() {
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(duplicateExternalIdHttpClientException()));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @ParameterizedTest(name = "create bad request ({0}) — skips product")
        @MethodSource("com.backbase.stream.investment.service.InvestmentPortfolioProductServiceTest#createBadRequestNotDuplicateExternalIdCases")
        void createBadRequestNotDuplicateExternalId_skipsProduct(String scenario, String responseBody) {
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListByExternalIdReturnsEmpty(externalId, ProductTypeEnum.ROBO_ADVISOR);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "Bad Request", null,
                    responseBody != null ? responseBody.getBytes() : null, null)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("patch fails with WebClientResponseException — falls back to existing product")
        void patchFailsWithWebClientResponse_fallsBackToExisting() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            PortfolioProduct existing = buildApiProduct(existingUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1);
            stubListReturnsProducts(ProductTypeEnum.SELF_TRADING, existing);

            when(investmentRestProductPortfolioService.updatePortfolioProduct(any(), any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.CONFLICT.value(), "Conflict", HttpHeaders.EMPTY,
                    "{\"errors\":{\"external_id\":[\"already exists\"]}}".getBytes(), StandardCharsets.UTF_8)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), "Self Trading"))))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("patch fails with unexpected exception — skips product")
        void patchFailsWithUnexpectedException_skipsProduct() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            PortfolioProduct existing = buildApiProduct(existingUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1);
            stubListReturnsProducts(ProductTypeEnum.SELF_TRADING, existing);

            when(investmentRestProductPortfolioService.updatePortfolioProduct(any(), any(), any()))
                .thenReturn(Mono.error(new IllegalStateException("patch failed")));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), "Self Trading"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create fails with WebClientResponseException — skips product")
        void createFailsWithWebClientResponse_skipsProduct() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error", HttpHeaders.EMPTY, null,
                    StandardCharsets.UTF_8)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), null))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create fails with HttpClientErrorException body — skips product")
        void createFailsWithHttpClientErrorBody_skipsProduct() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST, "Bad Request", null, "{\"code\":\"INVALID_INPUT\"}".getBytes(), null)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), null))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create duplicate externalId (WebClient 400, null body) — skips product")
        void createDuplicateExternalId_webClientNullBody_skipsProduct() {
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListByExternalIdReturnsEmpty(externalId, ProductTypeEnum.ROBO_ADVISOR);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY, null,
                    StandardCharsets.UTF_8)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create duplicate externalId (HttpClient non-400) — skips product")
        void createDuplicateExternalId_httpClientNonBadRequest_skipsProduct() {
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListByExternalIdReturnsEmpty(externalId, ProductTypeEnum.ROBO_ADVISOR);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(HttpClientErrorException.create(
                    HttpStatus.CONFLICT, "Conflict", null,
                    "{\"errors\":{\"external_id\":[\"already exists\"]}}".getBytes(), null)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("create duplicate externalId (WebClient non-400) — skips product")
        void createDuplicateExternalId_webClientNonBadRequest_skipsProduct() {
            String externalId = "ext-balanced-robo-advisor-004";
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            template.setExternalId(externalId);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListByExternalIdReturnsEmpty(externalId, ProductTypeEnum.ROBO_ADVISOR);
            stubListReturnsEmpty(ProductTypeEnum.ROBO_ADVISOR);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                    HttpStatus.CONFLICT.value(), "Conflict", HttpHeaders.EMPTY,
                    "{\"errors\":{\"external_id\":[\"already exists\"]}}".getBytes(), StandardCharsets.UTF_8)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products).isEmpty())
                .verifyComplete();
        }

        @Test
        @DisplayName("arrangement portfolio name not found — falls back to lowest order product")
        void arrangementPortfolioNameNotFound_fallsBackToLowestOrder() {
            UUID lowOrderUuid = UUID.randomUUID();
            UUID highOrderUuid = UUID.randomUUID();
            ProductPortfolio lowOrderTemplate = buildTemplate("Low Order Product", ProductTypeEnum.SELF_TRADING);
            ProductPortfolio highOrderTemplate = buildTemplate("High Order Product", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder()
                .portfolioProducts(List.of(lowOrderTemplate, highOrderTemplate))
                .build();

            InvestmentArrangement arrangement = InvestmentArrangement.builder()
                .name("Arrangement")
                .productTypeExternalId(ProductTypeEnum.SELF_TRADING.getValue())
                .productPortfolioName("Non Matching Name")
                .build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            PortfolioProduct lowOrder = buildApiProduct(lowOrderUuid, "Low Order Product", ProductTypeEnum.SELF_TRADING, 1);
            PortfolioProduct highOrder = buildApiProduct(highOrderUuid, "High Order Product", ProductTypeEnum.SELF_TRADING, 2);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.just(lowOrder), Mono.just(highOrder));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products).hasSize(2))
                .verifyComplete();

            assertThat(arrangement.getInvestmentProductId()).isEqualTo(lowOrderUuid);
        }

        @Test
        @DisplayName("name search paginates — finds product on second page")
        void nameSearchPaginates_findsProductOnSecondPage() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildTemplate("Balanced", ProductTypeEnum.ROBO_ADVISOR);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            PortfolioProduct existing = buildApiProduct(existingUuid, "Balanced", ProductTypeEnum.ROBO_ADVISOR, 13);
            PaginatedPortfolioProductList firstPage = new PaginatedPortfolioProductList()
                .count(1)
                .next(URI.create("http://next-page"))
                .results(List.of(buildApiProduct(UUID.randomUUID(), "Other", ProductTypeEnum.ROBO_ADVISOR, 1)));
            PaginatedPortfolioProductList secondPage = new PaginatedPortfolioProductList()
                .count(1)
                .results(List.of(existing));

            when(productsApi.listPortfolioProducts(
                eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(ORDERING),
                eq(List.of(ProductTypeEnum.ROBO_ADVISOR.getValue())), isNull(), isNull()))
                .thenReturn(Mono.just(firstPage));
            when(productsApi.listPortfolioProducts(
                eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
                isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE), isNull(), eq(ORDERING),
                eq(List.of(ProductTypeEnum.ROBO_ADVISOR.getValue())), eq(List.of("default")), isNull()))
                .thenReturn(Mono.just(secondPage));

            when(investmentRestProductPortfolioService.updatePortfolioProduct(
                eq(existingUuid.toString()), eq(List.of(ALLOCATION_ASSET_EXPAND)), any(ProductPortfolio.class)))
                .thenReturn(Mono.just(existing));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), "Balanced"))))
                .assertNext(products -> assertThat(products.getFirst().getUuid()).isEqualTo(existingUuid))
                .verifyComplete();
        }

        @Test
        @DisplayName("no matching product type for arrangement — arrangement product id stays null")
        void noMatchingProductType_arrangementNotAssigned() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();
            InvestmentArrangement arrangement = buildArrangement(
                ProductTypeEnum.ROBO_ADVISOR.getValue(), null);

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            UUID productUuid = UUID.randomUUID();
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.just(buildApiProduct(
                    productUuid, "Self Trading", ProductTypeEnum.SELF_TRADING, 1)));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(arrangement)))
                .assertNext(products -> assertThat(products).hasSize(1))
                .verifyComplete();

            assertThat(arrangement.getInvestmentProductId()).isNull();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ProductPortfolio buildTemplate(String name, ProductTypeEnum productType) {
        ProductPortfolio template = new ProductPortfolio();
        template.setName(name);
        template.setProductType(productType);
        template.setProductCategory("default");
        return template;
    }

    private PortfolioProduct buildApiProduct(
        UUID uuid, String name, ProductTypeEnum productType, Integer order) {
        return new PortfolioProduct(
            name, null, null, order, null, "default", uuid, null, null, productType);
    }

    private InvestmentArrangement buildArrangement(String productTypeExternalId, String productPortfolioName) {
        return InvestmentArrangement.builder()
            .name("Test Arrangement")
            .productTypeExternalId(productTypeExternalId)
            .productPortfolioName(productPortfolioName)
            .build();
    }

    private void stubListReturnsEmpty(ProductTypeEnum productType) {
        PaginatedPortfolioProductList emptyPage = new PaginatedPortfolioProductList()
            .count(0)
            .results(Collections.emptyList());
        when(productsApi.listPortfolioProducts(
            eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
            isNull(), isNull(), isNull(), isNull(), isNull(), eq(ORDERING),
            eq(List.of(productType.getValue())), isNull(), isNull()))
            .thenReturn(Mono.just(emptyPage));
    }

    private void stubListReturnsProducts(ProductTypeEnum productType, PortfolioProduct... products) {
        PaginatedPortfolioProductList page = new PaginatedPortfolioProductList()
            .count(products.length)
            .results(List.of(products));
        when(productsApi.listPortfolioProducts(
            eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
            isNull(), isNull(), isNull(), isNull(), isNull(), eq(ORDERING),
            eq(List.of(productType.getValue())), isNull(), isNull()))
            .thenReturn(Mono.just(page));
    }

    private void stubListByExternalIdReturns(String externalId, ProductTypeEnum productType,
        PortfolioProduct product) {
        PaginatedPortfolioProductList page = new PaginatedPortfolioProductList()
            .count(1)
            .results(List.of(product));
        when(productsApi.listPortfolioProducts(
            eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), eq(externalId), isNull(), eq(1),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(List.of(productType.getValue())), isNull(), isNull()))
            .thenReturn(Mono.just(page));
    }

    private void stubListByExternalIdReturnsEmpty(String externalId, ProductTypeEnum productType) {
        PaginatedPortfolioProductList emptyPage = new PaginatedPortfolioProductList()
            .count(0)
            .results(Collections.emptyList());
        when(productsApi.listPortfolioProducts(
            eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), eq(externalId), isNull(), eq(1),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(List.of(productType.getValue())), isNull(), isNull()))
            .thenReturn(Mono.just(emptyPage));
    }

    private void stubListByExternalIdReturnsEmptyThenProduct(String externalId, ProductTypeEnum productType,
        PortfolioProduct product) {
        PaginatedPortfolioProductList emptyPage = new PaginatedPortfolioProductList()
            .count(0)
            .results(Collections.emptyList());
        PaginatedPortfolioProductList page = new PaginatedPortfolioProductList()
            .count(1)
            .results(List.of(product));
        when(productsApi.listPortfolioProducts(
            eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), eq(externalId), isNull(), eq(1),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(List.of(productType.getValue())), isNull(), isNull()))
            .thenReturn(Mono.just(emptyPage), Mono.just(page));
    }

    private HttpClientErrorException duplicateExternalIdHttpClientException() {
        return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null,
            "{\"errors\":{\"external_id\":[\"Portfolio Product with this external id already exists.\"]}}".getBytes(),
            null);
    }

    private WebClientResponseException duplicateExternalIdWebClientException() {
        return WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY,
            "{\"errors\":{\"external_id\":[\"Portfolio Product with this external id already exists.\"]}}".getBytes(),
            StandardCharsets.UTF_8);
    }

}
