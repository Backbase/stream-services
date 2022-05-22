package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.compositions.legalentity.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityPostIngestionService;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.backbase.stream.compositions.product.client.model.ProductPullIngestionRequest;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class LegalEntityPostIngestionServiceImpl implements LegalEntityPostIngestionService {

    private BootstrapConfigurationProperties bootstrapConfigurationProperties;
    private EventBus eventBus;
    private LegalEntityConfigurationProperties legalEntityConfigurationProperties;

    private ProductCompositionApi productCompositionApi;


    @Override
    public void handleSuccess(LegalEntityResponse res) {
        log.info("Legal entities ingestion completed successfully.");
        if (Boolean.TRUE.equals(legalEntityConfigurationProperties.getChains().getProductComposition().getEnableOnComplete())) {
            log.info("Call product-composition-service for Legal Entity {}", res.getLegalEntity().getInternalId());
            sendProductPullEvent(res);
        }

        if (log.isDebugEnabled()) {
            log.debug("Ingested legal entity: {}", res.getLegalEntity());
        }


    }

    public void handleFailure(LegalEntity legalEntity) {
        if (Boolean.TRUE.equals(legalEntityConfigurationProperties.getChains().getProductComposition().getEnableOnComplete())) {
            log.info("Call product-composition-service for Legal Entity {}", legalEntity.getInternalId());
            sendProductPullEvent(legalEntity);
        }



    }

    private void sendProductPullEvent(LegalEntityResponse res) {
        if (CollectionUtils.isEmpty(legalEntity.getUsers())) {
            log.error("Legalentity is missing users. Cannot call product-composition");
            return;
        }

        ProductPullIngestionRequest productPullIngestionRequest =
                new ProductPullIngestionRequest().withLegalEntityExternalId(legalEntity.getExternalId())
                        .withServiceAgreementExternalId(legalEntity.getMasterServiceAgreement().getExternalId())
                        .withServiceAgreementInternalId(legalEntity.getMasterServiceAgreement().getInternalId())
                        .withMembershipAccounts();


        productPullIngestionRequest.setUserExternalId(
                legalEntity.getUsers().get(0).getUser().getExternalId());

        productCompositionApi.pullIngestProduct(productPullIngestionRequest)
                .onErrorResume(e -> {
                    log.info(e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
