package com.backbase.stream.worker.repository;

import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.UnitOfWork;

import org.springframework.data.repository.reactive.ReactiveSortingRepository;

import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

public interface UnitOfWorkRepository<T extends StreamTask, ID>
        extends ReactiveSortingRepository<UnitOfWork<T>, ID> {

    Flux<UnitOfWork<T>> findAllByRegisteredAtBefore(OffsetDateTime currentDateTime);

    Flux<UnitOfWork<T>> findAllByNextAttemptAtBefore(OffsetDateTime currentDateTime);
}
