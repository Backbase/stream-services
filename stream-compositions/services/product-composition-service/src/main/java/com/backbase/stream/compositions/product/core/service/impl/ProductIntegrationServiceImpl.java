package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class ProductIntegrationServiceImpl implements ProductIntegrationService {
    private final ProductIntegrationApi productIntegrationApi;

    private final ProductGroupMapper mapper;

    /**
     * {@inheritDoc}
     */
    public Mono<ProductIngestResponse> pullProductGroup(ProductIngestPullRequest ingestPullRequest) {
        return productIntegrationApi
                .pullProductGroup(
                        mapper.mapStreamToIntegration(ingestPullRequest))
                .map(mapper::mapResponseIntegrationToStream)
                .onErrorResume(this::handleIntegrationError)
                .flatMap(this::handleIntegrationResponse);

    }

    private Mono<ProductIngestResponse> handleIntegrationResponse(ProductIngestResponse res) {
        log.debug("Savings Accounts received from Integration: {}", res.getProductGroup().getSavingAccounts());
        log.debug("Current Accounts received from Integration: {}", res.getProductGroup().getCurrentAccounts());
        log.debug("Loan Accounts received from Integration: {}", res.getProductGroup().getLoans());
        log.debug("Credit Cards received from Integration: {}", res.getProductGroup().getCreditCards());
        log.debug("Debit Cards received from Integration: {}", res.getProductGroup().getDebitCards());
        log.debug("Investment accounts received from Integration: {}", res.getProductGroup().getInvestmentAccounts());
        log.debug("Term Deposit Accounts received from Integration: {}", res.getProductGroup().getTermDeposits());
        log.debug("Custom Accounts received from Integration: {}", res.getProductGroup().getCustomProducts());
        log.debug("Custom Data group items received from Integration: {}", res.getProductGroup().getCustomDataGroupItems());
        return Mono.just(res);
    }

    private Mono<ProductIngestResponse> handleIntegrationError(Throwable e) {
        log.error("Error while pulling products: {}", e.getMessage());
        return Mono.error(new InternalServerErrorException().withMessage(e.getMessage()));
    }
}
