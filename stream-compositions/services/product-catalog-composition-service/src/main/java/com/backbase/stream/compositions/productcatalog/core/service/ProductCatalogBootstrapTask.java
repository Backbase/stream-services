package com.backbase.stream.compositions.productcatalog.core.service;

import com.backbase.stream.compositions.productcatalog.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bootstrap.enabled", matchIfMissing = false)
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
public class ProductCatalogBootstrapTask implements ApplicationRunner {
    private final ReactiveProductCatalogService productCatalogService;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

    @Override
    public void run(ApplicationArguments args) {
        ProductCatalog productCatalog = bootstrapConfigurationProperties.getProductCatalog();
        bootstrapProductCatalog(productCatalog).subscribe();
    }

    private Mono<ProductCatalog> bootstrapProductCatalog(ProductCatalog productCatalog) {
        if (Objects.isNull(productCatalog)) {
            log.warn("Failed to load product catalog.");
            return Mono.empty();
        } else {
            log.info("Bootstrapping product catalog.");
            return productCatalogService.setupProductCatalog(productCatalog);
        }
    }
}
