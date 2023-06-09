package com.backbase.stream.compositions.product.core.model;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ArrangementIngestPushRequest {

    private AccountArrangementItemPut arrangement;
    private String arrangementInternalId;
    private String source;
    private RequestConfig config;
}
