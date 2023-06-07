package com.backbase.stream.audiences;

import com.backbase.stream.worker.StreamTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class CustomersSegmentSaga implements StreamTaskExecutor<CustomersSegmentTask> {

    public static final String ENTITY = "CustomerOnboarded";
    public static final String INGEST = "ingest";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String INGESTED_SUCCESSFULLY = "Customers ingested successfully";
    public static final String FAILED_TO_INGEST_CONTACTS = "Failed to ingest customers";

    @Override
    public Mono<CustomersSegmentTask> executeTask(CustomersSegmentTask streamTask) {
        return Mono.just(streamTask);
    }

    @Override
    public Mono<CustomersSegmentTask> rollBack(CustomersSegmentTask streamTask) {
        return Mono.just(streamTask);
    }
}
