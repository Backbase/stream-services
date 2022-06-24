package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.*;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ProductIngestResponse {
    private final ProductGroup productGroup;
    @With
    private Map<String, String> additions;
}
