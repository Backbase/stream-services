package com.backbase.stream.compositions.product.core.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArrangementIngestPullRequest {
  private String arrangementId;
  private String externalArrangementId;
  private String source;
  private RequestConfig config;
}
