package com.backbase.stream.compositions.product.core.mapper;

import com.backbase.stream.compositions.product.api.model.ArrangementIngestionResponse;
import com.backbase.stream.compositions.product.api.model.ArrangementPullIngestionRequest;
import com.backbase.stream.compositions.product.api.model.ArrangementPushIngestionRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPullRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestPushRequest;
import com.backbase.stream.compositions.product.core.model.ArrangementIngestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArrangementRestMapper {
    private final ArrangementMapper arrangementMapper;
    private final ConfigMapper chainsMapper;

    public ArrangementIngestPushRequest mapPushRequest(ArrangementPushIngestionRequest request) {
        return ArrangementIngestPushRequest.builder()
                .arrangement(arrangementMapper.mapCompositionToStream(request.getArrangement()))
                .source(request.getSource())
                .config(chainsMapper.map(request.getConfig()))
                .build();
    }

    public ArrangementIngestPullRequest mapPullRequest(ArrangementPullIngestionRequest request) {
        return ArrangementIngestPullRequest.builder()
                .arrangementId(request.getArrangementId())
                .externalArrangementId(request.getExternalArrangementId())
                .source(request.getSource())
                .config(chainsMapper.map(request.getConfig()))
                .build();
    }

    public ResponseEntity<ArrangementIngestionResponse> mapResponse(ArrangementIngestResponse response) {
        return new ResponseEntity<>(
                new ArrangementIngestionResponse()
                        .withArrangement(
                                arrangementMapper.mapStreamToComposition(response.getArrangement())
                        ),
                HttpStatus.CREATED);
    }
}