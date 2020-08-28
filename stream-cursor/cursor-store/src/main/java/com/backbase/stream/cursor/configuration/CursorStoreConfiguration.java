package com.backbase.stream.cursor.configuration;

import com.backbase.stream.cursor.CursorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

/**
 * Cursor Source Configuration.
 */
@Configuration
public class CursorStoreConfiguration {



    @Bean
    public CursorService cursorService(ReactiveMongoTemplate reactiveMongoTemplate) {
        return new CursorService(reactiveMongoTemplate);
    }
}
