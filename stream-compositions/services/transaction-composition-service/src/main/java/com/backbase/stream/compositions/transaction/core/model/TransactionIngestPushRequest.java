package com.backbase.stream.compositions.transaction.core.model;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TransactionIngestPushRequest {
  private String externalArrangementId;
  private String legalEntityInternalId;
  private String arrangementId;
  private List<TransactionsPostRequestBody> transactions;
}
