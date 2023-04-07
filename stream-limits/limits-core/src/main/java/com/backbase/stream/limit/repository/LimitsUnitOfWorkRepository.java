package com.backbase.stream.limit.repository;

import com.backbase.stream.limit.LimitsTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface LimitsUnitOfWorkRepository extends UnitOfWorkRepository<LimitsTask, String> {}
