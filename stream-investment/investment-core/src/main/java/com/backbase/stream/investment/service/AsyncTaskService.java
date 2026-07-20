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
            .flatMap(
                tick -> Flux.fromIterable(asyncTasks)
                    .flatMap(gr -> this.groupResultStatus(gr.getUuid()))
                    .collectList()
                    .doOnNext(results -> {
                        long pending = results.stream()
                            .filter(gr -> "PENDING".equalsIgnoreCase(gr.getStatus()))
                            .count();
                        if (pending > 0) {
                            log.info("Waiting for price async tasks: pending={}/{}", pending, asyncTasks.size());
                        }
                    })
            )
            .filter(results -> results.stream().noneMatch(gr -> "PENDING".equalsIgnoreCase(gr.getStatus())))
            .next()
            .timeout(java.time.Duration.ofMinutes(10))
            .doOnSuccess(tasks -> log.info("Prices tasks finished, added"))
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
