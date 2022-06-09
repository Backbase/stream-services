package com.backbase.stream.compositions.legalentity.core.service.impl;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.legalentity.model.LegalEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
@ConditionalOnProperty(name = "bootstrap.enabled")
public class RootLegalEntityBootstrapTask implements ApplicationRunner {
    private final LegalEntitySaga legalEntitySaga;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;
    private final EventBus eventBus;

    @Override
    public void run(ApplicationArguments args) {
        LegalEntity rootLegalEntity = bootstrapConfigurationProperties.getLegalEntity();
        bootstrapRootLegalEntity(rootLegalEntity)
                .subscribe();


    }

    @PostConstruct
    public void eventTest() {

    }


    private Mono<String> bootstrapRootLegalEntity(LegalEntity rootLegalEntity) {
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
