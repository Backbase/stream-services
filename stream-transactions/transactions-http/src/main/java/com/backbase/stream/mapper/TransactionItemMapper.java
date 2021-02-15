package com.backbase.stream.mapper;

import com.backbase.dbs.transaction.api.service.v2.model.ArrangementItem;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionItem;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsDeleteRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPatchRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostRequestBody;
import com.backbase.dbs.transaction.api.service.v2.model.TransactionsPostResponseBody;
import com.backbase.stream.transaction.inbound.model.TransactionPostResponse;
import com.backbase.stream.transaction.inbound.model.TransactionsPostTransactionPost;
import org.mapstruct.Mapper;

@Mapper
public interface TransactionItemMapper {

    com.backbase.stream.transaction.inbound.model.TransactionItem toInbound(TransactionItem transactionItem);

    TransactionPostResponse toInbound(TransactionsPostResponseBody transactionIds);

    TransactionsPatchRequestBody toPresentation(
        com.backbase.stream.transaction.inbound.model.TransactionsPatchRequestBody transactionsPatchRequestBody);

    TransactionsDeleteRequestBody toPresentation(
        com.backbase.stream.transaction.inbound.model.TransactionsDeleteRequestBody transactionsDeleteRequestBody);

    ArrangementItem toPresentation(com.backbase.stream.transaction.inbound.model.ArrangementItem inbound);

    TransactionsPostRequestBody toPresentation(TransactionsPostTransactionPost transactionsPostTransactionPost);
}
