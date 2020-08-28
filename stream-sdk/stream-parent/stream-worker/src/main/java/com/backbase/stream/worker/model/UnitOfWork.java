package com.backbase.stream.worker.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.util.CollectionUtils;

@Data
@Slf4j
public class UnitOfWork<T extends StreamTask> {

    public static <T extends StreamTask> UnitOfWork<T> from(String unitOfOWorkId, T task) {
        return from(unitOfOWorkId, Collections.singletonList(task));
    }

    public static <T extends StreamTask> UnitOfWork<T> from(String unitOfOWorkId, List<T> tasks) {
        UnitOfWork<T> unitOfWork = new UnitOfWork<T>();
        unitOfWork.setUnitOfOWorkId(unitOfOWorkId);
        unitOfWork.setStreamTasks(tasks);
        unitOfWork.setState(State.NEW);
        unitOfWork.setRegisteredAt(OffsetDateTime.now());
        unitOfWork.setNextAttemptAt(OffsetDateTime.now());
        return unitOfWork;
    }

    public static <T extends StreamTask> boolean isUnLocked(UnitOfWork<T> unitOfWork) {
        return unitOfWork.getLockedAt() == null;
    }

    public enum State {
        NEW,
        ACCEPTED,
        IN_PROGRESS,
        FAILED,
        FAILED_RETRIES_EXHAUSTED,
        COMPLETED,
        ROLLBACK_IN_PROGRESS,
        ROLLBACK_COMPLETED,
        ROLLBACK_FAILED,
    }

    @Id
    private String unitOfOWorkId;

    private List<T> streamTasks;

    private State state = State.NEW;

    private OffsetDateTime registeredAt;
    private OffsetDateTime lockedAt;
    private OffsetDateTime nextAttemptAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;

    private int retries = 0;

    public void logSummary() {
        if (log.isDebugEnabled()) {
            log.debug("UnitOfWork: {} Started at: {} Finished at: {} State: {}",
                unitOfOWorkId,
                startedAt,
                finishedAt,
                state
            );
        }
    }

}
