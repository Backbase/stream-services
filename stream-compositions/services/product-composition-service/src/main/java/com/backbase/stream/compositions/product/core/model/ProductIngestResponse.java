package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.*;

import java.util.Map;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class ProductIngestResponse {

    private String legalEntityExternalId;
    private String legalEntityInternalId;
    private String userExternalId;
    private String userInternalId;

    private final ProductGroup productGroup;
    @With
    private Map<String, String> additions;
}
