package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.core.mapper.ProductGroupMapper;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.ProductIngestResponse;
import com.backbase.stream.compositions.legalentity.core.service.ProductIngestionService;
import com.backbase.stream.compositions.product.api.ProductCompositionApi;
import com.backbase.stream.compositions.product.model.IngestionResponse;
import com.backbase.stream.compositions.product.model.PullIngestionRequest;
import com.backbase.stream.compositions.product.model.PushIngestionRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
public class ProductController implements ProductCompositionApi {
    private final ProductIngestionService productIngestionService;
    private final ProductGroupMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<IngestionResponse> pullIngestProductGroup(@Valid PullIngestionRequest pullIngestionRequest) {
        ProductIngestResponse response = productIngestionService.ingestPull(buildRequest(pullIngestionRequest)).block();
        return ResponseEntity.ok(buildResponse(response));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<IngestionResponse> pushIngestProductGroup(@Valid PushIngestionRequest pushIngestionRequest) {
        ProductIngestResponse response = productIngestionService.ingestPush(buildRequest(pushIngestionRequest)).block();
        return ResponseEntity.ok(buildResponse(response));
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return LegalEntityIngestPullRequest
     */
    private ProductIngestPullRequest buildRequest(PullIngestionRequest request) {
        return ProductIngestPullRequest.builder()
                .legalEntityExternalId(request.getLegalEntityExternalId())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return ProductIngestPushRequest
     */
    private ProductIngestPushRequest buildRequest(PushIngestionRequest request) {
        return ProductIngestPushRequest.builder()
                .productGroup(mapper.mapCompostionToStream(request.getProductGgroup()))
                .build();
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response ProductIngestResponse
     * @return IngestionResponse
     */
    private IngestionResponse buildResponse(ProductIngestResponse response) {
        return new IngestionResponse()
                .withProductGgroup(mapper.mapStreamToComposition(response.getProductGroup()));
    }
}
