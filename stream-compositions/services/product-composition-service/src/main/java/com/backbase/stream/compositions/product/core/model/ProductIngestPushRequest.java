package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProductIngestPushRequest {
    private String eventId;
    private String source;
    private ProductGroup productGroup;
}
