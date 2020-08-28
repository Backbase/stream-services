package com.backbase.stream.cursor.source;

import com.backbase.stream.cursor.CursorStreamService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.MessageProducer;

/**
 * Source Binding that publishes ingestion cursors on a Message Broker.
 */
@EnableBinding(Source.class)
@Slf4j
@AllArgsConstructor
public class CursorSourceBinding {

    private final Source source;
    private final CursorStreamService cursorStreamService;
    private final CursorSourceProperties cursorSourceProperties;

    @Bean
    public MessageProducer ingestionSourceMessageProducer() {
        CursorMessageProducer cursorMessageProducer = new CursorMessageProducer(cursorStreamService, cursorSourceProperties);
        cursorMessageProducer.setOutputChannel(source.output());
        return cursorMessageProducer;
    }

}
