package com.backbase.stream.compositions.product.core.model;

import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ArrangementIngestPullRequest {
    private String arrangementId;
    private String externalArrangementId;
    private String source;
    ProductConfigurationProperties.Chains config;
}
