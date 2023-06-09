package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.With;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ProductIngestResponse {

    private final List<ProductGroup> productGroups;
    private String legalEntityExternalId;
    private String legalEntityInternalId;
    private String userExternalId;
    private String userInternalId;

    private String serviceAgreementExternalId;
    private String serviceAgreementInternalId;
    @With
    private Map<String, String> additions;
    private String source;
    private Boolean transactionChainEnabledFromRequest;
    private Boolean paymentOrderChainEnabledFromRequest;

    public ProductIngestResponse(
        String serviceAgreementExternalId,
        String serviceAgreementInternalId,
        List<ProductGroup> productGroups,
        Map<String, String> additions) {
        this.serviceAgreementExternalId = serviceAgreementExternalId;
        this.serviceAgreementInternalId = serviceAgreementInternalId;
        this.productGroups = productGroups;
        this.additions = additions;
    }
}
