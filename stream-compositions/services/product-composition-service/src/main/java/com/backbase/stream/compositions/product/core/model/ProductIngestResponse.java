package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.*;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ProductIngestResponse {

    public ProductIngestResponse(ProductGroup productGroup, Map<String, String> additions) {
        this.productGroup = productGroup;
        this.additions = additions;
    }

    private String legalEntityExternalId;
    private String legalEntityInternalId;
    private String userExternalId;
    private String userInternalId;

    private final ProductGroup productGroup;
    @With
    private Map<String, String> additions;
}
