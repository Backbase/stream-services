package com.backbase.stream.worker.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;

@Data
@Slf4j
public class UnitOfWork<T extends StreamTask> {

  @Id private String unitOfOWorkId;
  private List<T> streamTasks;
  private State state = State.NEW;
  private OffsetDateTime registeredAt;
  private OffsetDateTime lockedAt;
  private OffsetDateTime nextAttemptAt;
  private OffsetDateTime startedAt;
  private OffsetDateTime finishedAt;
  private int retries = 0;

  public static <T extends StreamTask> UnitOfWork<T> from(String unitOfOWorkId, T task) {
    return from(unitOfOWorkId, Collections.singletonList(task));
  }

  /**
   * Create Unit Of Work for list of tasks.
   *
   * @param unitOfOWorkId The ID of the unit of work
   * @param tasks The list of Stream Tasks to execute
   * @param <T> Class extending StreamTask
   * @return A Unit of Work
   */
  public static <T extends StreamTask> UnitOfWork<T> from(String unitOfOWorkId, List<T> tasks) {
    UnitOfWork<T> unitOfWork = new UnitOfWork<T>();
    unitOfWork.setUnitOfOWorkId(unitOfOWorkId);
    unitOfWork.setStreamTasks(tasks);
    unitOfWork.setState(State.NEW);
    unitOfWork.setRegisteredAt(OffsetDateTime.now());
    unitOfWork.setNextAttemptAt(OffsetDateTime.now());
    return unitOfWork;
  }

  /** Outputs the summary of the unit of work to the log in debug. */
  public void logSummary() {
    if (log.isDebugEnabled()) {
      log.debug(
          "UnitOfWork: {} Started at: {} Finished at: {} State: {}",
          unitOfOWorkId,
          startedAt,
          finishedAt,
          state);
    }
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
}
