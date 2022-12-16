package com.backbase.stream.compositions.product.core.service.impl;

import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import com.backbase.stream.compositions.product.core.service.ArrangementIngestionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ArrangementIngestionServiceImpl implements ArrangementIngestionService {
    @Override
    public Mono<ArrangementIngestResponse> ingestPull(
            ArrangementIngestPullRequest ingestionRequest) {
        return null;
    }

    @Override
    public Mono<ArrangementIngestResponse> ingestPush(
            ArrangementIngestPushRequest ingestPushRequest) {
        return null;
    }
}
