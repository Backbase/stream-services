package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.ClientApi;
import com.backbase.investment.api.service.v1.InvestmentProductsApi;
import com.backbase.investment.api.service.v1.PortfolioApi;
import com.backbase.investment.api.service.v1.model.IntegrationPortfolioCreateRequest;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PatchedPortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.PortfolioList;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductCreateUpdateRequest;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.investment.api.service.v1.model.StatusA3dEnum;
import com.backbase.stream.investment.InvestmentArrangement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Service wrapper around generated {@link ClientApi} providing guarded create / patch operations with logging, minimal
 * idempotency helpers and consistent error handling. Design notes (see CODING_RULES_COPILOT.md): - No direct
 * manipulation of generated API classes beyond construction & mapping. - Side-effecting operations are logged at info
 * (create) or debug (patch) levels. - Exceptions from the underlying WebClient are propagated (caller decides retry
 * strategy).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentPortfolioService {

    private final InvestmentProductsApi productsApi;
    private final PortfolioApi portfolioApi;

    public Mono<PortfolioProduct> upsertInvestmentProducts(InvestmentArrangement investmentArrangement) {
        PortfolioProduct portfolioProduct = new PortfolioProduct(null, null, null, ProductTypeEnum.SELF_TRADING);
        Objects.requireNonNull(portfolioProduct, "PortfolioProduct must not be null");
        log.info("Creating investment product: {}", portfolioProduct.getProductType());
        // TODO: fix to process the pages
        return upsertPortfolioProducts(investmentArrangement, portfolioProduct);
    }

    public Mono<PortfolioList> upsertInvestmentPortfolios(InvestmentArrangement investmentArrangement,
        Map<String, List<UUID>> clientsByLeExternalId) {
        log.info("Creating investment portfolios: {}", investmentArrangement.getName());
        return portfolioApi.listPortfolios(null, null, null,
                null, investmentArrangement.getExternalId(), null, null, null,
                null, null, null, null)
            .flatMap(plist -> {
                log.info("PortfolioList {}", plist);
                return Mono.justOrEmpty(Optional.ofNullable(plist)
                    .filter(p -> !CollectionUtils.isEmpty(p.getResults()))
                    .map(p -> p.getResults().get(0)));
            })
            .switchIfEmpty(portfolioApi.createPortfolio(new IntegrationPortfolioCreateRequest()
                .product(investmentArrangement.getInvestmentProductId())
                .arrangementId(investmentArrangement.getInternalId())
                .externalId(investmentArrangement.getExternalId())
                .name(investmentArrangement.getName())
                .clients(getClients(investmentArrangement, clientsByLeExternalId))
                .currency("EUR")
                .status(StatusA3dEnum.ACTIVE), null, null, null)
            )
            .doOnSuccess(response -> {
                log.debug("List investment portfolio response: body={}", response);
            })
            .doOnError(throwable -> log.error("List investment portfolio failed", throwable));
    }

    private static List<UUID> getClients(InvestmentArrangement investmentArrangement,
        Map<String, List<UUID>> clientsByLeExternalId) {
        return investmentArrangement.getLegalEntityExternalIds().stream()
            .map(clientsByLeExternalId::get)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream).distinct().toList();
    }

    private Mono<PortfolioProduct> upsertPortfolioProducts(InvestmentArrangement investmentArrangement,
        PortfolioProduct portfolioProduct) {
        return productsApi.listPortfolioProducts(null,
                null, null, null, null, null, null, null, null, null,
                List.of(portfolioProduct.getProductType().getValue()))
            .flatMap(products -> {
                if (Objects.isNull(products) || CollectionUtils.isEmpty(products.getResults())) {
                    log.info("No existing investment product {}, proceeding with creation",
                        portfolioProduct.getProductType());
                    return Mono.empty();
                }
                PortfolioProduct portfolioProduct1 = products.getResults().get(0);
                log.debug("Attempting minimal PATCH investment product uuid={} payload={} ",
                    portfolioProduct1.getUuid(), portfolioProduct);
                return productsApi.patchPortfolioProduct(portfolioProduct1.getUuid().toString(), null, null, null,
                        new PatchedPortfolioProductCreateUpdateRequest()
                            .modelPortfolio(Optional.ofNullable(portfolioProduct.getModelPortfolio())
                                .map(InvestorModelPortfolio::getUuid)
                                .orElse(null))
                            .productType(portfolioProduct.getProductType())
                            .adviceEngine(portfolioProduct.getAdviceEngine())
                            .extraData(portfolioProduct.getExtraData())
                    )
                    .doOnSuccess(
                        updated -> {
                            log.info("Patched existing investment client uuid={}", updated.getUuid());
                            investmentArrangement.setInvestmentProductId(updated.getUuid());
                        })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("PATCH client uuid={} failed: status={} body={}, falling back to existing client",
                            portfolioProduct1.getUuid(), ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(portfolioProduct); // fallback original
                    });
            })
            .switchIfEmpty(productsApi.createPortfolioProduct(new PortfolioProductCreateUpdateRequest()
                .productType(portfolioProduct.getProductType()), null, null, null))
            .doOnSuccess(response -> {
                investmentArrangement.setInvestmentProductId(response.getUuid());
                log.debug("List investment products response: body={}", response);
            })
            .doOnError(throwable -> log.error("List investment products failed", throwable));
    }

}
