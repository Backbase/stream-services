package com.backbase.stream.worker;

import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import reactor.core.publisher.Mono;

public interface StreamTaskExecutor<T extends StreamTask> {

    Mono<T> executeTask(T streamTask);

    Mono<T> rollBack(T streamTask);

}
