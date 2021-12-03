package com.backbase.stream.compositions.legalentity.core;

import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
public class RootLegalEntityBootstrapTask implements ApplicationRunner {
    private final LegalEntitySaga legalEntitySaga;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

    @Override
    public void run(ApplicationArguments args) {
        LegalEntity rootLegalEntity = bootstrapConfigurationProperties.getLegalEntity();
        bootstrapTcuRootLegalEntity(rootLegalEntity)
                .subscribe();
    }

    private Mono<String> bootstrapTcuRootLegalEntity(LegalEntity rootLegalEntity) {
        if (Objects.isNull(rootLegalEntity)) {
            log.warn("Failed to load root legal entity.");
            return Mono.empty();
        } else {
            log.debug("Bootstrapping root legal entity: {}.", rootLegalEntity.getName());

            return legalEntitySaga.executeTask(new LegalEntityTask(rootLegalEntity))
                    .map(task -> task.getData().getInternalId())
                    .doOnError(Exception.class, e -> log.error("Failed to bootstrap root legal entity.", e))
                    .doOnSuccess(result -> log.info("Root legal entity bootstrapping complete. Internal ID: {}.", result));
        }
    }
}
