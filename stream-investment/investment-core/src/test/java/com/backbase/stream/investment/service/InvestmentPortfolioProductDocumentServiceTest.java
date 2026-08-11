package com.backbase.stream.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.OASDocumentResponse;
import com.backbase.investment.api.service.sync.v1.model.PaginatedOASDocumentResponseList;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.model.PaginatedPortfolioProductDocumentList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductDocumentLinkRequest;
import com.backbase.investment.api.service.v1.model.PortfolioProductDocumentResponse;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.investment.ProductPortfolio;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class InvestmentPortfolioProductDocumentServiceTest {

    private static final int CONTENT_RETRIEVE_LIMIT = 100;

    @Mock
    private InvestmentProductsApi productsApi;

    @Mock
    private ContentApi contentApi;

    private InvestmentPortfolioProductDocumentService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InvestmentPortfolioProductDocumentService(productsApi, contentApi);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("linkProductDocuments")
    class LinkProductDocumentsTests {

        @Test
        @DisplayName("no document names — skips linking")
        void noDocumentNames_skipsLinking() {
            ProductPortfolio template = new ProductPortfolio();
            template.setName("Self Trading");
            PortfolioProduct product = buildProduct(UUID.randomUUID(), "Self Trading");

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();

            verify(contentApi, never()).listContentDocuments(any(), any(), any(), any(), any(), any());
            verify(productsApi, never()).bulkCreatePortfolioProductDocuments(any(), any());
        }

        @Test
        @DisplayName("missing links — bulk creates only unresolved document links")
        void missingLinks_bulkCreatesDocuments() {
            UUID productUuid = UUID.randomUUID();
            UUID kiidUuid = UUID.randomUUID();
            UUID factsheetUuid = UUID.randomUUID();
            String kiidName = "Key investor information document - Opportunity Horizon Fund";
            String factsheetName = "Factsheet - Opportunity Horizon Fund";

            ProductPortfolio template = new ProductPortfolio();
            template.setName("Opportunity Horizon Fund");
            template.setDocument(kiidName + "\n" + factsheetName);

            PortfolioProduct product = buildProduct(productUuid, "Opportunity Horizon Fund");

            stubContentDocumentLookup(kiidName, kiidUuid);
            stubContentDocumentLookup(factsheetName, factsheetUuid);
            when(productsApi.listPortfolioProductDocuments(eq(productUuid), eq(CONTENT_RETRIEVE_LIMIT), eq(0)))
                .thenReturn(Mono.just(new PaginatedPortfolioProductDocumentList().count(0).results(List.of())));

            when(productsApi.bulkCreatePortfolioProductDocuments(eq(productUuid), any()))
                .thenReturn(Flux.just(
                    new PortfolioProductDocumentResponse(kiidUuid),
                    new PortfolioProductDocumentResponse(factsheetUuid)));

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();

            ArgumentCaptor<List<PortfolioProductDocumentLinkRequest>> captor = ArgumentCaptor.forClass(List.class);
            verify(productsApi).bulkCreatePortfolioProductDocuments(eq(productUuid), captor.capture());
            assertThat(captor.getValue()).extracting(PortfolioProductDocumentLinkRequest::getDocument)
                .containsExactly(kiidUuid, factsheetUuid);
        }

        @Test
        @DisplayName("all links already exist — skips bulk create")
        void allLinksExist_skipsBulkCreate() {
            UUID productUuid = UUID.randomUUID();
            UUID kiidUuid = UUID.randomUUID();
            UUID factsheetUuid = UUID.randomUUID();
            String kiidName = "Key investor information document - Opportunity Horizon Fund";
            String factsheetName = "Factsheet - Opportunity Horizon Fund";

            ProductPortfolio template = new ProductPortfolio();
            template.setDocument(kiidName + "\n" + factsheetName);
            PortfolioProduct product = buildProduct(productUuid, "Opportunity Horizon Fund");

            stubContentDocumentLookup(kiidName, kiidUuid);
            stubContentDocumentLookup(factsheetName, factsheetUuid);
            when(productsApi.listPortfolioProductDocuments(eq(productUuid), eq(CONTENT_RETRIEVE_LIMIT), eq(0)))
                .thenReturn(Mono.just(new PaginatedPortfolioProductDocumentList()
                    .count(2)
                    .results(List.of(
                        new PortfolioProductDocumentResponse(kiidUuid),
                        new PortfolioProductDocumentResponse(factsheetUuid)))));

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();

            verify(productsApi, never()).bulkCreatePortfolioProductDocuments(any(), any());
        }

        @Test
        @DisplayName("partial existing links — bulk creates only missing documents")
        void partialExistingLinks_bulkCreatesMissingOnly() {
            UUID productUuid = UUID.randomUUID();
            UUID kiidUuid = UUID.randomUUID();
            UUID factsheetUuid = UUID.randomUUID();
            String kiidName = "Key investor information document - Opportunity Horizon Fund";
            String factsheetName = "Factsheet - Opportunity Horizon Fund";

            ProductPortfolio template = new ProductPortfolio();
            template.setDocument(kiidName + "\n" + factsheetName);
            PortfolioProduct product = buildProduct(productUuid, "Opportunity Horizon Fund");

            stubContentDocumentLookup(kiidName, kiidUuid);
            stubContentDocumentLookup(factsheetName, factsheetUuid);
            when(productsApi.listPortfolioProductDocuments(eq(productUuid), eq(CONTENT_RETRIEVE_LIMIT), eq(0)))
                .thenReturn(Mono.just(new PaginatedPortfolioProductDocumentList()
                    .count(1)
                    .results(List.of(new PortfolioProductDocumentResponse(kiidUuid)))));

            when(productsApi.bulkCreatePortfolioProductDocuments(eq(productUuid), any()))
                .thenReturn(Flux.just(new PortfolioProductDocumentResponse(factsheetUuid)));

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();

            ArgumentCaptor<List<PortfolioProductDocumentLinkRequest>> captor = ArgumentCaptor.forClass(List.class);
            verify(productsApi).bulkCreatePortfolioProductDocuments(eq(productUuid), captor.capture());
            assertThat(captor.getValue()).extracting(PortfolioProductDocumentLinkRequest::getDocument)
                .containsExactly(factsheetUuid);
        }

        @Test
        @DisplayName("unresolved document name — skips bulk create")
        void unresolvedDocumentName_skipsBulkCreate() {
            ProductPortfolio template = new ProductPortfolio();
            template.setDocument("UNKNOWN DOC");
            PortfolioProduct product = buildProduct(UUID.randomUUID(), "Opportunity Horizon Fund");

            when(contentApi.listContentDocuments(isNull(), eq(CONTENT_RETRIEVE_LIMIT), eq("UNKNOWN DOC"), eq(0),
                isNull(), isNull()))
                .thenReturn(new PaginatedOASDocumentResponseList().count(0).results(List.of()));

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();

            verify(productsApi, never()).listPortfolioProductDocuments(any(), any(), any());
            verify(productsApi, never()).bulkCreatePortfolioProductDocuments(any(), any());
        }

        @Test
        @DisplayName("partial document resolution — bulk creates only resolved documents")
        void partialDocumentResolution_bulkCreatesResolvedOnly() {
            UUID productUuid = UUID.randomUUID();
            UUID kiidUuid = UUID.randomUUID();
            String kiidName = "Key investor information document - Opportunity Horizon Fund";
            String factsheetName = "Factsheet - Opportunity Horizon Fund";

            ProductPortfolio template = new ProductPortfolio();
            template.setDocument(kiidName + "\n" + factsheetName);
            PortfolioProduct product = buildProduct(productUuid, "Opportunity Horizon Fund");

            stubContentDocumentLookup(kiidName, kiidUuid);
            when(contentApi.listContentDocuments(isNull(), eq(CONTENT_RETRIEVE_LIMIT), eq(factsheetName), eq(0),
                isNull(), isNull()))
                .thenReturn(new PaginatedOASDocumentResponseList().count(0).results(List.of()));
            when(productsApi.listPortfolioProductDocuments(eq(productUuid), eq(CONTENT_RETRIEVE_LIMIT), eq(0)))
                .thenReturn(Mono.just(new PaginatedPortfolioProductDocumentList().count(0).results(List.of())));
            when(productsApi.bulkCreatePortfolioProductDocuments(eq(productUuid), any()))
                .thenReturn(Flux.just(new PortfolioProductDocumentResponse(kiidUuid)));

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();

            ArgumentCaptor<List<PortfolioProductDocumentLinkRequest>> captor = ArgumentCaptor.forClass(List.class);
            verify(productsApi).bulkCreatePortfolioProductDocuments(eq(productUuid), captor.capture());
            assertThat(captor.getValue()).extracting(PortfolioProductDocumentLinkRequest::getDocument)
                .containsExactly(kiidUuid);
        }

        @Test
        @DisplayName("bulk create failure — product upsert still completes")
        void bulkCreateFailure_productStillReturned() {
            UUID productUuid = UUID.randomUUID();
            String documentName = "Factsheet - Opportunity Horizon Fund";
            UUID documentUuid = UUID.randomUUID();

            ProductPortfolio template = new ProductPortfolio();
            template.setDocument(documentName);
            PortfolioProduct product = buildProduct(productUuid, "Opportunity Horizon Fund");

            stubContentDocumentLookup(documentName, documentUuid);
            when(productsApi.listPortfolioProductDocuments(eq(productUuid), eq(CONTENT_RETRIEVE_LIMIT), eq(0)))
                .thenReturn(Mono.just(new PaginatedPortfolioProductDocumentList().count(0).results(List.of())));
            when(productsApi.bulkCreatePortfolioProductDocuments(eq(productUuid), any()))
                .thenReturn(Flux.error(new RuntimeException("link failed")));

            StepVerifier.create(service.linkProductDocuments(template, product))
                .expectNext(product)
                .verifyComplete();
        }
    }

    private void stubContentDocumentLookup(String documentName, UUID documentUuid) {
        when(contentApi.listContentDocuments(isNull(), eq(CONTENT_RETRIEVE_LIMIT), eq(documentName), eq(0), isNull(),
            isNull()))
            .thenReturn(new PaginatedOASDocumentResponseList()
                .count(1)
                .results(List.of(new OASDocumentResponse(documentUuid).name(documentName))));
    }

    private PortfolioProduct buildProduct(UUID uuid, String name) {
        return new PortfolioProduct(name, null, null, 1, null, "savings-plan", uuid, null, null,
            ProductTypeEnum.SAVINGS_PLAN);
    }
}
