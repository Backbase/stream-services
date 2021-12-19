package com.nackbase.stream.compositions.productcatalog.core.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductCatalogBootstrapTaskTest {
    @Mock
    ReactiveProductCatalogService reactiveProductCatalogService;

    @Test
    void testProductCatalogSetup() {
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
}
