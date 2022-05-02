package com.backbase.stream.compositions.legalentity.http;

import com.backbase.stream.compositions.legalentity.api.LegalEntityCompositionApi;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityIngestionResponse;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPullIngestionRequest;
import com.backbase.stream.compositions.legalentity.api.model.LegalEntityPushIngestionRequest;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.backbase.stream.compositions.product.client.model.ProductPullIngestionRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@Slf4j
@EnableConfigurationProperties(LegalEntityConfigurationProperties.class)
public class LegalEntityController implements LegalEntityCompositionApi {
    private final LegalEntityIngestionService legalEntityIngestionService;
    private final LegalEntityMapper mapper;
    private final LegalEntityConfigurationProperties configProperties;
    private final ProductCompositionApi productCompositionApi;

    @Override
    public Mono<ResponseEntity<Void>> pullAsyncLegalEntity(Mono<LegalEntityPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        Mono.fromCallable(() -> legalEntityIngestionService.ingestPull(pullIngestionRequest.map(this::buildPullRequest)))
                .subscribeOn(Schedulers.boundedElastic()).subscribe();

        return Mono.empty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<LegalEntityIngestionResponse>> pullLegalEntity(
            @Valid Mono<LegalEntityPullIngestionRequest> pullIngestionRequest, ServerWebExchange exchange) {
        log.info("Start synchronous ingestion of Legal Entity");
        return legalEntityIngestionService.ingestPull(pullIngestionRequest.map(this::buildPullRequest))
                .map(this::handleResponse)
                .map(this::mapIngestionToResponse);
    }

    @Override
    public Mono<ResponseEntity<Void>> pushAsyncLegalEntity(Mono<LegalEntityPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        legalEntityIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .subscribeOn(Schedulers.boundedElastic());
        return Mono.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<LegalEntityIngestionResponse>> pushLegalEntity(
            @Valid Mono<LegalEntityPushIngestionRequest> pushIngestionRequest, ServerWebExchange exchange) {
        return legalEntityIngestionService
                .ingestPush(pushIngestionRequest.map(this::buildPushRequest))
                .map(this::mapIngestionToResponse);
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PullIngestionRequest
     * @return LegalEntityIngestPullRequest
     */
    private LegalEntityPullRequest buildPullRequest(LegalEntityPullIngestionRequest request) {
        return LegalEntityPullRequest
                .builder()
                .legalEntityExternalId(request.getLegalEntityExternalId())
                .additionalParameters(request.getAdditionalParameters())
                .build();
    }

    /**
     * Builds ingestion request for downstream service.
     *
     * @param request PushIngestionRequest
     * @return LegalEntityIngestPushRequest
     */
    private LegalEntityPushRequest buildPushRequest(LegalEntityPushIngestionRequest request) {
        return LegalEntityPushRequest
                .builder()
                .legalEntity(mapper.mapCompostionToStream(request.getLegalEntity()))
                .build();
    }

    private LegalEntityResponse handleResponse(LegalEntityResponse response) {
        if (Boolean.TRUE.equals(configProperties.getChainProductEvent())) {
            log.info("Calling Product ingestion service api for LE {}", response.getLegalEntity().getExternalId());
            sendProductPullEvent(response);
        }
        return response;
    }

    /**
     * Builds ingestion response for API endpoint.
     *
     * @param response LegalEntityIngestResponse
     * @return ResponseEntity<IngestionResponse>
     */
    private ResponseEntity<LegalEntityIngestionResponse> mapIngestionToResponse(LegalEntityResponse response) {
        return new ResponseEntity<>(
                new LegalEntityIngestionResponse()
                        .withLegalEntity(mapper.mapStreamToComposition(response.getLegalEntity())),
                HttpStatus.CREATED);
    }

    private void sendProductPullEvent(LegalEntityResponse response) {
        log.info("Calling Product Composition API");
        ProductPullIngestionRequest productPullIngestionRequest =
                new ProductPullIngestionRequest().withLegalEntityExternalId("le1")
                .withServiceAgreementExternalId("sae1")
                .withServiceAgreementInternalId("sai1")
                .withUserExternalId("u1");

        productCompositionApi.pullIngestProductGroup(productPullIngestionRequest)
                .onErrorResume(e -> {
                    log.info(e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
