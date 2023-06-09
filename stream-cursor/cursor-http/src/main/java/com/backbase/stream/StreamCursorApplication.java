package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application exposing REST and Streaming over HTTP service for Ingestion Cursors.
 */
@SpringBootApplication
public class StreamCursorApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamCursorApplication.class, args);
    }
}
