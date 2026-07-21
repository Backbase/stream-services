package com.backbase.stream.investment.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.model.GroupResult;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("AsyncTaskService")
class AsyncTaskServiceTest {

    private AsyncBulkGroupsApi asyncBulkGroupsApi;
    private AsyncTaskService service;

    @BeforeEach
    void setUp() {
        asyncBulkGroupsApi = mock(AsyncBulkGroupsApi.class);
        service = new AsyncTaskService(asyncBulkGroupsApi);
    }

    @Nested
    @DisplayName("checkPriceAsyncTasksFinished")
    class CheckPriceAsyncTasksFinishedTests {

        @Test
        @DisplayName("empty task list — returns empty list immediately")
        void emptyTaskList_returnsEmptyList() {
            StepVerifier.create(service.checkPriceAsyncTasksFinished(List.of()))
                .expectNext(List.of())
                .verifyComplete();
        }

        @Test
        @DisplayName("all tasks completed on first poll — returns polled results")
        void allTasksCompletedOnFirstPoll_returnsPolledResults() {
            UUID uuid = UUID.randomUUID();
            GroupResult inputTask = new GroupResult(uuid, "PENDING", List.of());
            GroupResult completed = new GroupResult(uuid, "COMPLETED", List.of());

            when(asyncBulkGroupsApi.getBulkGroup(uuid.toString())).thenReturn(Mono.just(completed));

            StepVerifier.withVirtualTime(() -> service.checkPriceAsyncTasksFinished(List.of(inputTask)))
                .thenAwait(Duration.ofSeconds(5))
                .expectNextMatches(results -> results.size() == 1
                    && "COMPLETED".equalsIgnoreCase(results.getFirst().getStatus()))
                .verifyComplete();
        }

        @Test
        @DisplayName("tasks transition from pending to completed — polls until finished")
        void tasksTransitionFromPendingToCompleted_pollsUntilFinished() {
            UUID uuid = UUID.randomUUID();
            GroupResult inputTask = new GroupResult(uuid, "PENDING", List.of());
            GroupResult completed = new GroupResult(uuid, "COMPLETED", List.of());

            when(asyncBulkGroupsApi.getBulkGroup(uuid.toString()))
                .thenReturn(Mono.just(new GroupResult(uuid, "PENDING", List.of())))
                .thenReturn(Mono.just(completed));

            StepVerifier.withVirtualTime(() -> service.checkPriceAsyncTasksFinished(List.of(inputTask)))
                .thenAwait(Duration.ofSeconds(10))
                .expectNextMatches(results -> results.size() == 1
                    && "COMPLETED".equalsIgnoreCase(results.getFirst().getStatus()))
                .verifyComplete();
        }

        @Test
        @DisplayName("pending status is case-insensitive — completes when status is lowercase")
        void pendingStatusCaseInsensitive_completesWhenNoLongerPending() {
            UUID uuid = UUID.randomUUID();
            GroupResult inputTask = new GroupResult(uuid, "PENDING", List.of());
            GroupResult completed = new GroupResult(uuid, "completed", List.of());

            when(asyncBulkGroupsApi.getBulkGroup(uuid.toString()))
                .thenReturn(Mono.just(new GroupResult(uuid, "pending", List.of())))
                .thenReturn(Mono.just(completed));

            StepVerifier.withVirtualTime(() -> service.checkPriceAsyncTasksFinished(List.of(inputTask)))
                .thenAwait(Duration.ofSeconds(10))
                .expectNextMatches(results -> results.size() == 1
                    && "completed".equals(results.getFirst().getStatus()))
                .verifyComplete();
        }

        @Test
        @DisplayName("timeout waiting for tasks — returns original task list")
        void timeoutWaitingForTasks_returnsOriginalTaskList() {
            UUID uuid = UUID.randomUUID();
            GroupResult inputTask = new GroupResult(uuid, "PENDING", List.of());

            when(asyncBulkGroupsApi.getBulkGroup(uuid.toString()))
                .thenReturn(Mono.just(new GroupResult(uuid, "PENDING", List.of())));

            StepVerifier.withVirtualTime(() -> service.checkPriceAsyncTasksFinished(List.of(inputTask)))
                .thenAwait(Duration.ofMinutes(11))
                .expectNextMatches(results -> results.size() == 1
                    && uuid.equals(results.getFirst().getUuid()))
                .verifyComplete();
        }

        @Test
        @DisplayName("API error while polling — returns original task list")
        void apiErrorWhilePolling_returnsOriginalTaskList() {
            UUID uuid = UUID.randomUUID();
            GroupResult inputTask = new GroupResult(uuid, "PENDING", List.of());

            when(asyncBulkGroupsApi.getBulkGroup(uuid.toString()))
                .thenReturn(Mono.error(new RuntimeException("bulk group lookup failed")));

            StepVerifier.withVirtualTime(() -> service.checkPriceAsyncTasksFinished(List.of(inputTask)))
                .thenAwait(Duration.ofSeconds(5))
                .expectNextMatches(results -> results.size() == 1
                    && uuid.equals(results.getFirst().getUuid()))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("groupResultStatus")
    class GroupResultStatusTests {

        @Test
        @DisplayName("delegates to AsyncBulkGroupsApi")
        void delegatesToAsyncBulkGroupsApi() {
            UUID uuid = UUID.randomUUID();
            GroupResult groupResult = new GroupResult(uuid, "COMPLETED", List.of());

            when(asyncBulkGroupsApi.getBulkGroup(uuid.toString())).thenReturn(Mono.just(groupResult));

            StepVerifier.create(service.groupResultStatus(uuid))
                .expectNext(groupResult)
                .verifyComplete();

            verify(asyncBulkGroupsApi, times(1)).getBulkGroup(eq(uuid.toString()));
        }
    }
}
