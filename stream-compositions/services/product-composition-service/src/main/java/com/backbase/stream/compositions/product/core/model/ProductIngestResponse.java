package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductIngestResponse {
    private final ProductGroup productGroup;
}
