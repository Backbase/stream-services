package com.backbase.stream.compositions.product.core.model;

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
    private RequestConfig config;
}
