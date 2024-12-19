package com.backbase.stream.compositions.product.core.model;

import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ArrangementIngestPushRequest {
    private ArrangementPutItem arrangement;
    private String arrangementInternalId;
    private String source;
    private RequestConfig config;
}
