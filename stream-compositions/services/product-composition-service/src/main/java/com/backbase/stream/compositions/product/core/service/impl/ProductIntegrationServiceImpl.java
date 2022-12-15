package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.product.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.product.core.service.ProductIntegrationService;
import com.backbase.stream.legalentity.model.ProductGroup;
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
                .pullProductGroup(mapper.mapStreamToIntegration(ingestPullRequest))
                .map(mapper::mapResponseIntegrationToStream)
                .map(response -> this.setRequestParameters(ingestPullRequest, response))
                .onErrorResume(this::handleIntegrationError)
                .flatMap(this::handleIntegrationResponse);
    }

    /**
     * Sets serviceAgreementsIds, legalEntityIds, userIds from request to response.
     *
     * @param request  ProductIngestPullRequest
     * @param response ProductIngestResponse
     * @return ProductIngestResponse
     */
    private ProductIngestResponse setRequestParameters(
            ProductIngestPullRequest request, ProductIngestResponse response) {
        response.setServiceAgreementInternalId(request.getServiceAgreementInternalId());
        response.setServiceAgreementExternalId(request.getServiceAgreementExternalId());
        response.setLegalEntityExternalId(request.getLegalEntityExternalId());
        response.setLegalEntityInternalId(request.getLegalEntityInternalId());
        response.setUserExternalId(request.getUserExternalId());
        response.setUserInternalId(request.getUserInternalId());
        response.setSource(request.getSource());
        response.setAdditions(request.getAdditions());
        return response;
    }

    private Mono<ProductIngestResponse> handleIntegrationResponse(ProductIngestResponse res) {
        for (ProductGroup productGroup : res.getProductGroups()) {
            log.debug("Product Group: " + productGroup.getName());
            log.debug("Savings Accounts received from Integration: {}", productGroup.getSavingAccounts());
            log.debug("Current Accounts received from Integration: {}", productGroup.getCurrentAccounts());
            log.debug("Loan Accounts received from Integration: {}", productGroup.getLoans());
            log.debug("Credit Cards received from Integration: {}", productGroup.getCreditCards());
            log.debug("Debit Cards received from Integration: {}", productGroup.getDebitCards());
            log.debug("Investment accounts received from Integration: {}", productGroup.getInvestmentAccounts());
            log.debug("Term Deposit Accounts received from Integration: {}", productGroup.getTermDeposits());
            log.debug("Custom Accounts received from Integration: {}", productGroup.getCustomProducts());
            log.debug("Custom Data group items received from Integration: {}", productGroup.getCustomDataGroupItems());
        }
        return Mono.just(res);
    }

    private Mono<ProductIngestResponse> handleIntegrationError(Throwable e) {
        log.error("Error while pulling products: {}", e.getMessage());
        return Mono.error(new InternalServerErrorException().withMessage(e.getMessage()));
    }
}
