package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.sync.v1.ContentApi;
import com.backbase.investment.api.service.sync.v1.model.OASDocumentResponse;
import com.backbase.investment.api.service.sync.v1.model.PaginatedOASDocumentResponseList;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductDocumentLinkRequest;
import com.backbase.investment.api.service.v1.model.PortfolioProductDocumentResponse;
import com.backbase.stream.investment.ProductPortfolio;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Links bootstrap content documents to portfolio products via the Investment Products API.
 */
@Slf4j
@RequiredArgsConstructor
public class InvestmentPortfolioProductDocumentService {

    private static final int CONTENT_RETRIEVE_LIMIT = 100;

    private final InvestmentProductsApi productsApi;
    private final ContentApi contentApi;

    public static InvestmentPortfolioProductDocumentService noOp() {
        return new InvestmentPortfolioProductDocumentService(null, null) {
            @Override
            public Mono<PortfolioProduct> linkProductDocuments(ProductPortfolio template, PortfolioProduct product) {
                if (product == null) {
                    return Mono.empty();
                }
                return Mono.just(product);
            }
        };
    }

    /**
     * Links content documents to a portfolio product when missing. Existing links are left unchanged.
     *
     * @param template bootstrap product template containing newline-separated content document names
     * @param product  upserted portfolio product
     * @return the unchanged product, or empty when linking is skipped
     */
    public Mono<PortfolioProduct> linkProductDocuments(ProductPortfolio template, PortfolioProduct product) {
        if (product == null) {
            return Mono.empty();
        }
        List<String> documentNames = parseDocumentNames(template != null ? template.getDocument() : null);
        if (documentNames.isEmpty()) {
            return Mono.just(product);
        }

        return Flux.fromIterable(documentNames)
            .concatMap(this::findDocumentUuidByName)
            .collectList()
            .flatMap(documentUuids -> createMissingDocumentLinks(product, documentNames, documentUuids))
            .onErrorResume(error -> {
                log.warn(
                    "Continuing without portfolio product document links: productUuid={}, name={}, reason={}",
                    product.getUuid(), product.getName(), error.getMessage());
                return Mono.just(product);
            });
    }

    private Mono<PortfolioProduct> createMissingDocumentLinks(PortfolioProduct product, List<String> documentNames,
        List<UUID> documentUuids) {
        List<UUID> resolvedUuids = documentUuids.stream().filter(Objects::nonNull).toList();
        if (resolvedUuids.isEmpty()) {
            return Mono.just(product);
        }

        return loadExistingLinkedDocumentUuids(product.getUuid())
            .flatMap(existingLinkedUuids -> {
                List<UUID> missingDocumentUuids = resolvedUuids.stream()
                    .filter(documentUuid -> !existingLinkedUuids.contains(documentUuid))
                    .toList();

                if (missingDocumentUuids.isEmpty()) {
                    log.info(
                        "All content documents already linked to portfolio product: productUuid={}, name={}, "
                            + "documentUuids={}",
                        product.getUuid(), product.getName(), resolvedUuids);
                    return Mono.just(product);
                }

                List<PortfolioProductDocumentLinkRequest> linkRequests = missingDocumentUuids.stream()
                    .map(uuid -> new PortfolioProductDocumentLinkRequest().document(uuid))
                    .toList();

                log.info(
                    "Linking {} missing content document(s) to portfolio product: productUuid={}, name={}, "
                        + "documentUuids={}",
                    linkRequests.size(), product.getUuid(), product.getName(), missingDocumentUuids);

                return productsApi.bulkCreatePortfolioProductDocuments(product.getUuid(), linkRequests)
                    .collectList()
                    .doOnSuccess(responses -> log.info(
                        "Linked content documents to portfolio product: productUuid={}, name={}, linkedCount={}",
                        product.getUuid(), product.getName(), responses.size()))
                    .doOnError(error -> log.error(
                        "Failed to link content documents to portfolio product: productUuid={}, name={}, "
                            + "documentNames={}",
                        product.getUuid(), product.getName(), documentNames, error))
                    .thenReturn(product);
            });
    }

    private Mono<Set<UUID>> loadExistingLinkedDocumentUuids(UUID productUuid) {
        return loadExistingLinkedDocumentUuids(productUuid, 0, new HashSet<>());
    }

    private Mono<Set<UUID>> loadExistingLinkedDocumentUuids(UUID productUuid, int offset, Set<UUID> accumulated) {
        return productsApi.listPortfolioProductDocuments(productUuid, CONTENT_RETRIEVE_LIMIT, offset)
            .flatMap(page -> {
                if (page.getResults() != null) {
                    page.getResults().stream()
                        .filter(Objects::nonNull)
                        .map(PortfolioProductDocumentResponse::getUuid)
                        .forEach(accumulated::add);
                }
                if (page.getNext() == null) {
                    return Mono.just(accumulated);
                }
                return loadExistingLinkedDocumentUuids(productUuid, offset + CONTENT_RETRIEVE_LIMIT, accumulated);
            })
            .defaultIfEmpty(accumulated);
    }

    private List<String> parseDocumentNames(String documentField) {
        if (!StringUtils.hasText(documentField)) {
            return List.of();
        }
        return Arrays.stream(documentField.split("\\R"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private Mono<UUID> findDocumentUuidByName(String documentName) {
        return Mono.fromCallable(() -> lookupDocumentUuidByName(documentName))
            .flatMap(uuid -> uuid != null ? Mono.just(uuid) : Mono.empty());
    }

    private UUID lookupDocumentUuidByName(String documentName) {
        PaginatedOASDocumentResponseList page = contentApi.listContentDocuments(
            null, CONTENT_RETRIEVE_LIMIT, documentName, 0, null, null);
        if (page == null || page.getResults() == null || page.getResults().isEmpty()) {
            log.warn("Content document not found by name: name={}", documentName);
            return null;
        }

        return page.getResults().stream()
            .filter(Objects::nonNull)
            .filter(document -> documentName.equals(document.getName()))
            .map(OASDocumentResponse::getUuid)
            .findFirst()
            .orElseGet(() -> {
                log.warn("Content document name query returned no exact match: name={}", documentName);
                return null;
            });
    }

}
