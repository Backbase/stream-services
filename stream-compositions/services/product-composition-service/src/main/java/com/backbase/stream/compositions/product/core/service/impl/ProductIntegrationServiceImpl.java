package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.integration.product.api.ProductIntegrationApi;
import com.backbase.stream.compositions.integration.product.model.AvailableBalance;
import com.backbase.stream.compositions.integration.product.model.LegalEntityReference;
import com.backbase.stream.compositions.integration.product.model.ProductGroup;
import com.backbase.stream.compositions.integration.product.model.SavingsAccount;
import com.backbase.stream.compositions.product.core.model.ProductIngestPullRequest;
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

    /**
     * {@inheritDoc}
     */
    public Mono<ProductGroup> pullProductGroup(ProductIngestPullRequest ingestPullRequest) {
        LegalEntityReference leRef = new LegalEntityReference();
        leRef.setExternalId(ingestPullRequest.getLegalEntityExternalId());
        return Mono.just(new ProductGroup().name("Some Name")
                .addSavingAccountsItem(new SavingsAccount()
                        .name("Savings1")
                        .addLegalEntitiesItem(leRef)
                        .BBAN("bban")
                        .externalId("account1")
                        .productTypeExternalId("savings-account")
                        .availableBalance(new AvailableBalance().amount(1.01).currencyCode("USD"))));
                /*productIntegrationApi
                .pullProductGroup(
                        ingestPullRequest.getLegalEntityExternalId(),
                        ingestPullRequest.getServiceAgreementExternalId(),
                        ingestPullRequest.getServiceAgreementInternalId(),
                        ingestPullRequest.getUserExternalId(),
                        ingestPullRequest.getAdditionalParameters())
                .map(PullProductGroupResponse::getProductGroup);*/
    }
}
