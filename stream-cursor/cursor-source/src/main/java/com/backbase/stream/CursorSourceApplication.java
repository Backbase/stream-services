package com.backbase.stream;

import com.backbase.stream.cursor.configuration.CursorServiceConfiguration;
import com.backbase.stream.cursor.source.CursorSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({CursorSourceProperties.class})
@Import(CursorServiceConfiguration.class)
public class CursorSourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CursorSourceApplication.class, args);
    }

}

