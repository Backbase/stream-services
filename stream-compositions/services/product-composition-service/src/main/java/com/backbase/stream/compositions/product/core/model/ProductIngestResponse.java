package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ProductIngestResponse {
    private String serviceAgreementExternalId;
    private String serviceAgreementInternalId;
    private final List<ProductGroup> productGroups;
    @With
    private Map<String, String> additions;
}
