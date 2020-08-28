package com.backbase.stream.legalentity.generator;

import com.backbase.stream.legalentity.generator.configuration.LegalEntityGeneratorSourceConfigurationProperties;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.configuration.ProductCatalogServiceConfiguration;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class LegalEntityGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalEntityGeneratorApplication.class, args);
    }

}

@EnableBinding(Source.class)
@EnableConfigurationProperties(LegalEntityGeneratorSourceConfigurationProperties.class)
@Import(ProductCatalogServiceConfiguration.class)
@Slf4j
class LegalEntityGeneratorSourceBinding {

    private final LegalEntityGenerator legalEntityGenerator;
    private final LegalEntityGeneratorSourceConfigurationProperties configurationProperties;
    private final ReactiveProductCatalogService productCatalogService;
    private final Source source;

    LegalEntityGeneratorSourceBinding(LegalEntityGenerator legalEntityGenerator,
                                      LegalEntityGeneratorSourceConfigurationProperties configurationProperties,
                                      ReactiveProductCatalogService productCatalogService,
                                      Source source) {
        this.legalEntityGenerator = legalEntityGenerator;
        this.configurationProperties = configurationProperties;
        this.productCatalogService = productCatalogService;
        this.source = source;
    }

//    @StreamEmitter
//    @Output(Source.OUTPUT)
//    public Flux<Message<?>> emit() {
//        return Flux.create(engine);
//    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        Duration interval = Duration.of(configurationProperties.getDelay(), configurationProperties.getTimeUnit());
        log.info("Generating {} Legal Entities Every: {}", configurationProperties.getNumberOfLegalEntitiesToGeneratePerTrigger(), interval);

        Mono<ProductCatalog> productCatalogMono = getProductCatalog();

        Flux.interval(interval)
            .flatMap(i -> productCatalogMono)
            .map(legalEntityGenerator::generate)
            .bufferTimeout(configurationProperties.getNumberOfLegalEntitiesToGeneratePerTrigger(), interval)
            .map(this::publish)
            .onErrorResume(throwable -> {
                log.error("Failed to publish Legal Entities: {}", throwable.getMessage(), throwable);
                return Mono.empty();
            })
            .subscribe(legalEntities -> {
                log.info("Published {} Legal Entities", legalEntities.size());
            });

    }

    private List<LegalEntity> publish(List<LegalEntity> legalEntities) {
        MessageBuilder<List<LegalEntity>> messageBuilder = MessageBuilder.withPayload(legalEntities);
        Message<List<LegalEntity>> message = messageBuilder.build();
        source.output().send(message);

        return legalEntities;
    }

    public Mono<ProductCatalog> getProductCatalog() {
        return productCatalogService.getProductCatalog()
            .cache(Duration.ofMillis(50000));
    }


}

