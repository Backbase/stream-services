package com.nackbase.stream.compositions.productcatalog.core.service.impl;

import com.backbase.stream.compositions.productcatalog.core.config.BootstrapConfigurationProperties;
import com.backbase.stream.compositions.productcatalog.core.service.ProductCatalogBootstrapTask;
import com.backbase.stream.productcatalog.ReactiveProductCatalogService;
import com.backbase.stream.productcatalog.model.ProductCatalog;
import com.backbase.stream.productcatalog.model.ProductKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCatalogBootstrapTaskTest {
    @Mock
    ReactiveProductCatalogService reactiveProductCatalogService;

    @Test
    void testProductCatalogSetup_Success() {
        List<ProductKind> productKinds = new ArrayList<>();
        productKinds.add(new ProductKind().kindName("kindName"));
        ProductCatalog productCatalog = new ProductCatalog().productKinds(productKinds);

        BootstrapConfigurationProperties bootstrapConfigurationProperties = new BootstrapConfigurationProperties();
        bootstrapConfigurationProperties.setProductCatalog(new ProductCatalog().productKinds(productKinds));

        when(reactiveProductCatalogService.setupProductCatalog(any())).thenReturn(Mono.just(productCatalog));

        ProductCatalogBootstrapTask bootstrapTask = new ProductCatalogBootstrapTask(
                reactiveProductCatalogService,
                bootstrapConfigurationProperties);

        bootstrapTask.run(null);
        verify(reactiveProductCatalogService).setupProductCatalog(productCatalog);
    }

    @Test
    void testProductCatalogSetup_Fail() {
        BootstrapConfigurationProperties bootstrapConfigurationProperties = new BootstrapConfigurationProperties();
        bootstrapConfigurationProperties.setProductCatalog(null);

        ProductCatalogBootstrapTask bootstrapTask = new ProductCatalogBootstrapTask(
                reactiveProductCatalogService,
                bootstrapConfigurationProperties);

        bootstrapConfigurationProperties.setProductCatalog(null);
        bootstrapTask.run(null);
        verify(reactiveProductCatalogService, times(0)).setupProductCatalog(any());
    }
}
