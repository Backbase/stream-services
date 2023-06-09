package com.backbase.stream.compositions.transaction.cursor.core.repository;

import com.backbase.stream.compositions.transaction.cursor.core.domain.TransactionCursorEntity;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TransactionCursorRepository
    extends CrudRepository<TransactionCursorEntity, String>, TransactionCursorCustomRepository {

    /**
     * Query the cursor based on arrangement_id criteria
     *
     * @param arrangementId ArrangementId of the cursor
     * @return TransactionCursorEntity
     */
    Optional<TransactionCursorEntity> findByArrangementId(String arrangementId);

    /**
     * Delete the cursor based on arrangement_id
     *
     * @param arrangementId ArrangementId of the cursor
     */
    @Transactional(rollbackFor = SQLException.class)
    void deleteByArrangementId(String arrangementId);
}
