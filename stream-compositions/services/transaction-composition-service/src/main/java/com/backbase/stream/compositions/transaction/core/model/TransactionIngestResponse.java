package com.backbase.stream.compositions.transaction.core.model;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestResponse {
    private final List<TransactionsPostResponseBody> transactions;
}
