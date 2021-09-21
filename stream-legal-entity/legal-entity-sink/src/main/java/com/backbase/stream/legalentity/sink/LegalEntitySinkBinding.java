package com.backbase.stream.legalentity.sink;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.configuration.LegalEntitySagaConfiguration;
import com.backbase.stream.legalentity.model.LegalEntity;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;

@EnableBinding(Sink.class)
@Import(LegalEntitySagaConfiguration.class)
@Slf4j
public class LegalEntitySinkBinding {

    private final LegalEntitySaga legalEntitySaga;

    public LegalEntitySinkBinding(LegalEntitySaga legalEntitySaga) {
        this.legalEntitySaga = legalEntitySaga;
    }

    @StreamListener(Sink.INPUT)
    public void accept(List<LegalEntity> legalEntityAggregates) {

        Flux.fromIterable(legalEntityAggregates).map(LegalEntityTask::new)
            .flatMap(legalEntitySaga::executeTask)
            .subscribe(streamTask -> {
                if (streamTask.isCompleted()) {
                    log.info("Completed Stream Task: {}", streamTask.getName());
                } else {
                    log.error("Failed Stream Task: {}", streamTask.getName());
                }
            });

    }
}
