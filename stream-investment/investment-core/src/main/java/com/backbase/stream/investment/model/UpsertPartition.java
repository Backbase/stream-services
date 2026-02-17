package com.backbase.stream.investment.model;

/**
 * Partitions entities into those to be created and those to be updated during an upsert operation. Exactly one of
 * create or update must be non-null.
 *
 */
public record UpsertPartition<ID, T>(ID id, T entity) {

    public static <ID, T> UpsertPartition<ID, T> createPartition(T entity) {
        return new UpsertPartition<>(null, entity);
    }

}
