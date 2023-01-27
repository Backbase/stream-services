package com.backbase.stream.compositions.product.core.model;

import lombok.*;
import com.backbase.stream.legalentity.model.ProductGroup;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ProductIngestResponse {

    public ProductIngestResponse(String serviceAgreementExternalId,
                                 String serviceAgreementInternalId,
                                 List<ProductGroup> productGroups,
                                 Map<String, String> additions) {
        this.serviceAgreementExternalId = serviceAgreementExternalId;
        this.serviceAgreementInternalId = serviceAgreementInternalId;
        this.productGroups = productGroups;
        this.additions = additions;
    }

    private String legalEntityExternalId;
    private String legalEntityInternalId;
    private String userExternalId;
    private String userInternalId;

    private String serviceAgreementExternalId;
    private String serviceAgreementInternalId;

    private final List<ProductGroup> productGroups;
    @With
    private Map<String, String> additions;

    private String source;

    private Boolean transactionChainEnabledFromRequest;
    private Boolean paymentOrderChainEnabledFromRequest;

}
