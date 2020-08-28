package com.backbase.stream.product.sink;

import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.product.ProductIngestionSaga;
import com.backbase.stream.product.task.ProductGroupTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@EnableBinding(Sink.class)
@Slf4j
@AllArgsConstructor
public class ProductSinkBinding {

    private final ProductIngestionSaga productIngestionSaga;

    @StreamListener(Sink.INPUT)
    public void accept(ProductGroup productGroup) {
        log.info("Ingestion Product Group: {}", productGroup.getName());
        ProductGroupTask streamTask = new ProductGroupTask(productGroup.getName(), productGroup);
        streamTask.data(productGroup);

        productIngestionSaga.process(streamTask)
            .doOnNext(ingestedProductGroup -> {
                log.info("Ingested Product Group: {}", streamTask.getData().getName());
            })
            .block();
    }
}
