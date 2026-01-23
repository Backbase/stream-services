package com.backbase.stream.investment.service;

import com.backbase.investment.api.service.v1.AsyncBulkGroupsApi;
import com.backbase.investment.api.service.v1.model.GroupResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncBulkGroupsApi asyncBulkGroupsApi;

    public Mono<List<GroupResult>> checkPriceAsyncTasksFinished(List<GroupResult> asyncTasks) {
        log.info("Start checking for price ingest processing. Async tasks: {}", asyncTasks.size());
        if (asyncTasks.isEmpty()) {
            return Mono.just(List.of());
        }
        return Flux.interval(java.time.Duration.ofSeconds(5))
            // Poll the status of all tasks
            .flatMap(
                tick -> Flux.fromIterable(asyncTasks)
                    .flatMap(gr -> this.groupResultStatus(gr.getUuid()))
                    .collectList()
            )
            .filter(results -> results.stream().noneMatch(gr -> "PENDING".equalsIgnoreCase(gr.getStatus())))
            .next()
            .timeout(java.time.Duration.ofMinutes(5))
            .doOnSuccess(tasks -> {
                log.info("Prices tasks finished added");
                log.debug("Price async tasks failure: {}",
                    tasks.stream().filter(gr -> "FAILURE".equalsIgnoreCase(gr.getStatus())).toList());
            })
            .onErrorResume(throwable -> {
                log.error("Timeout or error waiting for GroupResult tasks: taskIds={}",
                    asyncTasks.stream().map(GroupResult::getUuid).toList(), throwable);
                return Mono.just(asyncTasks);
            });
    }

    public Mono<GroupResult> groupResultStatus(UUID uuid) {
        return asyncBulkGroupsApi.getBulkGroup(uuid.toString());
    }

}
