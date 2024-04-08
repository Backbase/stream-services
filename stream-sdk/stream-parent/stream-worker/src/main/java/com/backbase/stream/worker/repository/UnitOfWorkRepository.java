package com.backbase.stream.worker.repository;

import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.UnitOfWork;
import java.time.OffsetDateTime;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;

public interface UnitOfWorkRepository<T extends StreamTask, ID> extends ReactiveSortingRepository<UnitOfWork<T>, ID>,
    ReactiveCrudRepository<UnitOfWork<T>, ID> {

    Flux<UnitOfWork<T>> findAllByRegisteredAtBefore(OffsetDateTime currentDateTime);

    Flux<UnitOfWork<T>> findAllByNextAttemptAtBefore(OffsetDateTime currentDateTime);

}
