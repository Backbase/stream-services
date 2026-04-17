package com.backbase.stream.cursor.configuration;

import com.backbase.stream.cursor.CursorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Cursor Source Configuration.
 */
@Configuration
@EnableR2dbcRepositories
public class CursorStoreConfiguration {

    @Bean
    public CursorService cursorService(CursorRepository cursorRepository) {
        return new CursorService(cursorRepository);
    }

}
