package com.backbase.stream.worker.repository.impl;

import com.backbase.stream.worker.model.StreamTask;
import com.backbase.stream.worker.model.UnitOfWork;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public class InMemoryReactiveUnitOfWorkRepository<T extends StreamTask>
    implements UnitOfWorkRepository<T, String> {

  private ConcurrentHashMap<String, UnitOfWork<T>> inMemStorage = new ConcurrentHashMap<>();

  @Override
  public Flux<UnitOfWork<T>> findAllByRegisteredAtBefore(OffsetDateTime currentDateTime) {
    return Flux.fromStream(
        inMemStorage.values().stream()
            .filter(
                legalEntityUnitOfWork ->
                    legalEntityUnitOfWork.getRegisteredAt() != null
                        && legalEntityUnitOfWork.getRegisteredAt().isBefore(currentDateTime)));
  }

  @Override
  public Flux<UnitOfWork<T>> findAllByNextAttemptAtBefore(OffsetDateTime currentDateTime) {
    return Flux.fromStream(
        inMemStorage.values().stream()
            .filter(
                legalEntityUnitOfWork ->
                    legalEntityUnitOfWork.getNextAttemptAt() != null
                        && legalEntityUnitOfWork.getNextAttemptAt().isBefore(currentDateTime)));
  }

  @Override
  public <S extends UnitOfWork<T>> Mono<S> save(S entity) {
    if (entity.getUnitOfOWorkId() == null) {
      entity.setUnitOfOWorkId(UUID.randomUUID().toString());
    }
    inMemStorage.put(entity.getUnitOfOWorkId(), entity);
    return Mono.just(entity);
  }

  @Override
  public <S extends UnitOfWork<T>> Flux<S> saveAll(Iterable<S> entities) {
    return Flux.fromIterable(entities).flatMap(this::save);
  }

  @Override
  public <S extends UnitOfWork<T>> Flux<S> saveAll(Publisher<S> entityStream) {
    return Flux.from(entityStream).flatMap(this::save);
  }

  @Override
  public Mono<UnitOfWork<T>> findById(String unitOfWorkId) {
    return Mono.justOrEmpty(inMemStorage.get(unitOfWorkId));
  }

  @Override
  public Mono<UnitOfWork<T>> findById(Publisher<String> id) {
    return Mono.from(id).flatMap(this::findById);
  }

  @Override
  public Mono<Boolean> existsById(String s) {
    return Mono.just(inMemStorage.containsKey(s));
  }

  @Override
  public Mono<Boolean> existsById(Publisher<String> id) {
    return Mono.from(id).flatMap(this::existsById);
  }

  @Override
  public Flux<UnitOfWork<T>> findAll() {
    return Flux.fromStream(inMemStorage.values().stream());
  }

  @Override
  public Flux<UnitOfWork<T>> findAllById(Iterable<String> strings) {
    return findAll()
        .filter(
            unitOfWork ->
                CollectionUtils.contains(strings.iterator(), unitOfWork.getUnitOfOWorkId()));
  }

  @Override
  public Flux<UnitOfWork<T>> findAllById(Publisher<String> idStream) {
    return Flux.from(idStream).flatMap(this::findById);
  }

  @Override
  public Mono<Long> count() {
    return Mono.just((long) inMemStorage.size());
  }

  @Override
  public Mono<Void> deleteById(String s) {
    inMemStorage.remove(s);
    return Mono.empty();
  }

  @Override
  public Mono<Void> deleteById(Publisher<String> id) {
    return Mono.from(id).map(this::deleteById).then();
  }

  @Override
  public Mono<Void> delete(UnitOfWork<T> entity) {
    return this.deleteById(entity.getUnitOfOWorkId());
  }

  @Override
  public Mono<Void> deleteAllById(Iterable<? extends String> iterable) {
    return Flux.fromIterable(iterable).map(this::deleteById).then();
  }

  @Override
  public Mono<Void> deleteAll(Iterable<? extends UnitOfWork<T>> entities) {
    return Flux.fromIterable(entities).map(this::delete).then();
  }

  @Override
  public Mono<Void> deleteAll(Publisher<? extends UnitOfWork<T>> entityStream) {
    return Flux.from(entityStream).map(this::delete).then();
  }

  @Override
  public Mono<Void> deleteAll() {
    inMemStorage = new ConcurrentHashMap<>();
    return Mono.empty();
  }

  @Override
  public Flux<UnitOfWork<T>> findAll(Sort sort) {
    return findAll();
  }
}
