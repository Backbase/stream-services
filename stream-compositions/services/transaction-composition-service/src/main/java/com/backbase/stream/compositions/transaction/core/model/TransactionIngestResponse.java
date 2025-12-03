package com.backbase.stream.compositions.transaction.core.model;

import com.backbase.dbs.transaction.api.service.v3.model.TransactionsPostResponseBody;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestResponse {
    private final String arrangementId;
    private final List<TransactionsPostResponseBody> transactions;
    private final Map<String, String> additions;
}
