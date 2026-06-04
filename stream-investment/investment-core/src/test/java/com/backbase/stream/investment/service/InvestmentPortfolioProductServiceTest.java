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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
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
            investmentRestProductPortfolioService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
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
        @DisplayName("list API failure — propagates error")
        void listApiFailure_propagatesError() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            when(productsApi.listPortfolioProducts(
                eq(List.of(ALLOCATION_ASSET_EXPAND)), isNull(), isNull(), isNull(), eq(LIST_PRODUCT_PAGE_SIZE),
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(ORDERING),
                eq(List.of(ProductTypeEnum.SELF_TRADING.getValue())), isNull(), isNull()))
                .thenReturn(Mono.error(new RuntimeException("list failed")));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), null))))
                .expectError(RuntimeException.class)
                .verify();
        }

        @Test
        @DisplayName("create API failure — propagates error")
        void createApiFailure_propagatesError() {
            ProductPortfolio template = buildTemplate("Self Trading", ProductTypeEnum.SELF_TRADING);
            InvestmentData data = InvestmentData.builder().portfolioProducts(List.of(template)).build();

            stubListReturnsEmpty(ProductTypeEnum.SELF_TRADING);
            when(investmentRestProductPortfolioService.createPortfolioProduct(any(), any()))
                .thenReturn(Mono.error(new IllegalStateException("create failed")));

            StepVerifier.create(service.upsertInvestmentProducts(data, List.of(buildArrangement(
                ProductTypeEnum.SELF_TRADING.getValue(), null))))
                .expectError(IllegalStateException.class)
                .verify();
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
}
