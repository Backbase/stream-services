package com.backbase.stream.compositions.legalentity.core.service;

import com.backbase.stream.compositions.legalentity.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.productcatalog.ProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bootstrap.enabled", matchIfMissing = false)
@EnableConfigurationProperties(BootstrapConfigurationProperties.class)
public class ProuductCatalogBootstrapTask implements ApplicationRunner {
    private final ProductCatalogService productCatalogService;
    private final BootstrapConfigurationProperties bootstrapConfigurationProperties;

    @Override
    public void run(ApplicationArguments args) {
        ProductCatalog productCatalog = bootstrapConfigurationProperties.getProductCatalog();
        bootstrapProductCatalog(productCatalog);
    }

    private void bootstrapProductCatalog(ProductCatalog productCatalog) {
        if (Objects.isNull(productCatalog)) {
            log.warn("Failed to load product catalog.");
        } else {
            log.info("Bootstrapping product catalog.");
            productCatalogService.setupProductCatalog(productCatalog);
        }
    }
}
