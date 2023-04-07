package com.backbase.stream.cursor.configuration;

import com.backbase.stream.cursor.model.CursorItem;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.awt.*;

@Repository
public interface CursorRepository extends ReactiveCrudRepository<CursorItem, String> {}
