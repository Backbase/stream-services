package com.backbase.stream.compositions.product.core.model;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import com.backbase.stream.compositions.product.core.config.ProductConfigurationProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ArrangementIngestPushRequest {
    AccountArrangementItemPut arrangement;
    private String source;
    ProductConfigurationProperties.Chains config;
}
