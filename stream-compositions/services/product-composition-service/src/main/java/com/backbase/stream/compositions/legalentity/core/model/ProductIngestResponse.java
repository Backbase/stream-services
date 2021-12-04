package com.backbase.stream.compositions.legalentity.core.model;

import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ProductGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProductIngestResponse {
    private final ProductGroup productGroup;
}
