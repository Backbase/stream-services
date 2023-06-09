package com.backbase.stream.compositions.transaction.core.model;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestResponse {

  private final String arrangementId;
  private final List<TransactionsPostResponseBody> transactions;
}
