package com.backbase.stream.compositions.transaction.cursor.core.repository;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorFilterRequest;
import com.backbase.stream.compositions.transaction.cursor.model.TransactionCursorPatchRequest;

import java.sql.SQLException;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

public interface TransactionCursorCustomRepository {

    /**
     * Patch the Cursor based on arrangement_id
     *
     * @param arrangementId                 ArrangementId of the Cursor
     * @param transactionCursorPatchRequest Request Payload to Patch the Cursor
     */
    @Transactional(
            rollbackFor = SQLException.class
    )
    Integer patchByArrangementId(String arrangementId,
                                 TransactionCursorPatchRequest transactionCursorPatchRequest);

    /**
     * Filter the Cursor based on last_txn_date and status
     *
     * @param transactionCursorFilterRequest Request Payload to filter the Cursor
     */
    List<TransactionCursorEntity> filterCursor(
            TransactionCursorFilterRequest transactionCursorFilterRequest);
}
