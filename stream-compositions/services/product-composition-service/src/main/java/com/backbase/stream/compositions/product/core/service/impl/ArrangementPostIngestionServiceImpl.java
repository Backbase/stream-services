package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementPostIngestionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class ArrangementPostIngestionServiceImpl implements ArrangementPostIngestionService {
    @Override
    public Mono<ArrangementIngestResponse> handleSuccess(ArrangementIngestResponse response) {
        return null;
    }

    @Override
    public void handleFailure(Throwable error) {

    }
}
