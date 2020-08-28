package com.backbase.stream.mapper;

import com.backbase.dbs.transaction.presentation.service.model.ArrangementItem;
import com.backbase.dbs.transaction.presentation.service.model.TransactionIds;
import com.backbase.dbs.transaction.presentation.service.model.TransactionItem;
import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPatch;
import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
import com.backbase.dbs.transaction.presentation.service.model.TransactionsDeleteRequestBody;
import com.backbase.stream.transaction.inbound.model.TransactionPostResponse;
import com.backbase.stream.transaction.inbound.model.TransactionsPatchRequestBody;
import com.backbase.stream.transaction.inbound.model.TransactionsPostTransactionPost;
import org.mapstruct.Mapper;

@Mapper
public interface TransactionItemMapper {

    com.backbase.stream.transaction.inbound.model.TransactionItem toInbound(TransactionItem transactionItem);

    TransactionPostResponse toInbound(TransactionIds transactionIds);

    TransactionItemPatch toPresentation(TransactionsPatchRequestBody transactionsPatchRequestBody);

    TransactionsDeleteRequestBody toPresentation(com.backbase.stream.transaction.inbound.model.TransactionsDeleteRequestBody transactionsDeleteRequestBody);

    ArrangementItem toPresentation(com.backbase.stream.transaction.inbound.model.ArrangementItem inbound);

    TransactionItemPost toPresentation(TransactionsPostTransactionPost transactionsPostTransactionPost);
}
