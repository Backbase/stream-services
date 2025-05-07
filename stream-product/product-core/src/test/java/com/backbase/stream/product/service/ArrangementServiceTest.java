package com.backbase.stream.product.service;

import static java.util.stream.Collectors.toSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementIdentification;
import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementPost;
import com.backbase.dbs.arrangement.api.integration.v3.model.ArrangementStateIdentification;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchResponseItem;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchResponseStatusCode;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchUpsertResponse;
import com.backbase.dbs.arrangement.api.integration.v3.model.ErrorItem;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntitiesDelete;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntitiesListPost;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntityExternal;
import com.backbase.dbs.arrangement.api.integration.v3.model.LegalEntityIdentification;
import com.backbase.dbs.arrangement.api.integration.v3.model.ProductIdentification;
import com.backbase.dbs.arrangement.api.integration.v3.model.SubscriptionPost;
import com.backbase.dbs.arrangement.api.integration.v3.model.UuidResponse;
import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.dbs.arrangement.api.service.v3.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementSearchesListResponse;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsDeleteItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsDeleteItem.SelectorEnum;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsDeleteResponseElement;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementsSearchesPostRequest;
import com.backbase.stream.product.exception.ArrangementCreationException;
import com.backbase.stream.product.exception.ArrangementUpdateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ArrangementServiceTest {

    @InjectMocks
    private ArrangementService arrangementService;

    @Mock
    private ArrangementsApi arrangementsApi;

    @Mock
    private com.backbase.dbs.arrangement.api.integration.v3.ArrangementsApi arrangementsIntegrationApi;

    private static WebClientResponseException buildWebClientResponseException(HttpStatus httpStatus,
        String statusText) {
        return WebClientResponseException.create(httpStatus.value(), statusText, null, null, null);
    }

    private static ArrangementPost buildArrangementPost() {
        ArrangementPost postArrangement = new ArrangementPost();
        postArrangement.setExternalId("ext_arr_id");
        postArrangement.setLegalEntities(Set.of(
            new LegalEntityExternal().externalId("ext_leid_1"),
            new LegalEntityExternal().externalId("ext_leid_2")
        ));
        postArrangement.setProduct(new ProductIdentification().externalId("ext_prod_id"));
        postArrangement.setState(new ArrangementStateIdentification().externalId("ext_state_id"));
        postArrangement.setName("arr_name");
        return postArrangement;
    }

    private static ArrangementPutItem buildArrangementPutItem() {
        return new ArrangementPutItem()
            .externalArrangementId("ext_arr_id")
            .productId("prod_id");
    }

    // USER PREFERENCES UPDATE HAS BEEN REMOVED FROM V3 ENDPOINT ONWARDS BECAUSE USER PREFERENCES
    // UPDATE IS AN INTERNAL OPERATION
//    private static AccountUserPreferencesItemPut buildAccountUserPreferencesItemPut() {
//        return new AccountUserPreferencesItemPut()
//            .arrangementId("arr_id")
//            .userId("user_id");
//    }

    @Test
    void createArrangement() {
        ArrangementPost request = buildArrangementPost();

        UuidResponse uuidResponse = new UuidResponse().id("arr_response_id");
        when(arrangementsIntegrationApi.postArrangements(request)).thenReturn(Mono.just(uuidResponse));

        StepVerifier.create(arrangementService.createArrangement(request))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(uuidResponse.getId(), response.getId());
                Assertions.assertEquals(request.getProduct().getExternalId(), response.getExternalProductId());
                Assertions.assertNotNull(response.getState());
                Assertions.assertEquals(request.getState().getExternalId(), response.getState().getState());
                Assertions.assertEquals(getLegalEntityIds(request.getLegalEntities()), response.getLegalEntityIds());
            }).verifyComplete();

        verify(arrangementsIntegrationApi).postArrangements(request);
    }

    private Set<String> getLegalEntityIds(Set<LegalEntityExternal> legalEntityExternals) {
        if (legalEntityExternals == null) {
            return null;
        }
        if (legalEntityExternals.isEmpty()) {
            return Set.of();
        }
        return legalEntityExternals.stream().map(LegalEntityExternal::getExternalId).collect(Collectors.toSet());
    }

    @Test
    void createArrangement_Failure() {
        ArrangementPost request = buildArrangementPost();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request for create arrangement");

        when(arrangementsIntegrationApi.postArrangements(any())).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.createArrangement(request))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(ArrangementCreationException.class, e);
                Assertions.assertEquals("Failed to post arrangements", e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            }).verify();

        verify(arrangementsIntegrationApi).postArrangements(any());
    }

    @Test
    void updateArrangement() {
        String arrangementId = UUID.randomUUID().toString();
        ArrangementPutItem request = buildArrangementPutItem();

        when(arrangementsApi.putArrangementById(arrangementId, request)).thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.updateArrangement(arrangementId, request))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(request.getExternalArrangementId(), response.getExternalArrangementId());
                Assertions.assertEquals(request.getProductId(), response.getProductId());
            }).verifyComplete();

        verify(arrangementsApi).putArrangementById(arrangementId, request);
    }

    @Test
    void updateArrangement_Failure() {
        String arrangementId = UUID.randomUUID().toString();
        ArrangementPutItem request = buildArrangementPutItem();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request for update arrangement");
        when(arrangementsApi.putArrangementById(arrangementId, request)).thenReturn(
            Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.updateArrangement(arrangementId, request))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(ArrangementUpdateException.class, e);
                Assertions.assertEquals(
                    "Failed to update Arrangement: %s".formatted(request.getExternalArrangementId()), e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            }).verify();

        verify(arrangementsApi).putArrangementById(arrangementId, request);
    }

    @Test
    void upsertBatchArrangements() {
        ArrangementPost request = buildArrangementPost();

        BatchResponseItem batchResponseItem = new BatchResponseItem()
            .arrangementId("arr_id")
            .arrangementExternalId("resource_id")
            .status(BatchResponseStatusCode.HTTP_STATUS_OK);

        BatchUpsertResponse batchUpsertResponse = new BatchUpsertResponse()
            .addResultsItem(batchResponseItem);

        when(arrangementsIntegrationApi.postBatchUpsertArrangements(any())).thenReturn(Mono.just(batchUpsertResponse));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(batchResponseItem.getArrangementId(),
                    response.getArrangementId());
            }).verifyComplete();

        verify(arrangementsIntegrationApi).postBatchUpsertArrangements(any());
    }

    @Test
    void upsertBatchArrangements_Batch_Error() {
        ArrangementPost request = buildArrangementPost();

        BatchResponseItem batchResponseItem = new BatchResponseItem();
        batchResponseItem.setArrangementId("arr_id");
        batchResponseItem.setArrangementExternalId("resource_id");
        batchResponseItem.setStatus(BatchResponseStatusCode.HTTP_STATUS_BAD_REQUEST);
        batchResponseItem.addErrorsItem(new ErrorItem().message("Some error"));
        batchResponseItem.addErrorsItem(new ErrorItem().message("Some other error"));

        BatchUpsertResponse batchUpsertResponse = new BatchUpsertResponse()
            .addResultsItem(batchResponseItem);

        when(arrangementsIntegrationApi.postBatchUpsertArrangements(any())).thenReturn(Mono.just(batchUpsertResponse));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .consumeErrorWith(e -> {
                String errorMessage = e.getMessage();
                Assertions.assertTrue(errorMessage.startsWith("Batch arrangement update failed: 'resource_id'"));
                Assertions.assertTrue(errorMessage.contains("message: Some error"));
                Assertions.assertTrue(errorMessage.contains("message: Some other error"));
            }).verify();

        verify(arrangementsIntegrationApi).postBatchUpsertArrangements(any());
    }

    @Test
    void upsertBatchArrangements_Failure() {
        ArrangementPost request = buildArrangementPost();

        List<ArrangementPost> postArrangementList = List.of(request);
        List<String> last4ExtArrList = postArrangementList.stream()
            .map(arrangementItem -> arrangementItem.getName() + " | " + arrangementItem.getExternalId()
                .substring(arrangementItem.getExternalId().length() - 4))
            .toList();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request for upsert arrangement");
        when(arrangementsIntegrationApi.postBatchUpsertArrangements(any())).thenReturn(
            Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.upsertBatchArrangements(postArrangementList))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(ArrangementUpdateException.class, e);
                Assertions.assertEquals("Batch arrangement update failed for arrangements : " + last4ExtArrList,
                    e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            }).verify();

        verify(arrangementsIntegrationApi).postBatchUpsertArrangements(any());
    }

    @Test
    void getArrangement() {
        String internalId = "internal_id";

        ArrangementItem arrangementItem = new ArrangementItem().id("acct_arr_item_id");
        when(arrangementsApi.getArrangementById(internalId, false)).thenReturn(Mono.just(arrangementItem));

        StepVerifier.create(arrangementService.getArrangement(internalId))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(response.getId(), arrangementItem.getId());
            }).verifyComplete();

        verify(arrangementsApi).getArrangementById(internalId, false);
    }

    @Test
    void getArrangement_NotFound() {
        String internalId = "internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND,
            "Arrangement Not Found");
        when(arrangementsApi.getArrangementById(internalId, false)).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangement(internalId)).verifyComplete();

        verify(arrangementsApi).getArrangementById(internalId, false);
    }

    @Test
    void getArrangementByExternalId() {
        String externalId = "external_id";

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));

        ArrangementSearchesListResponse arrangementSearchesListResponse = new ArrangementSearchesListResponse();

        ArrangementItem arrangementItem = new ArrangementItem().id("acct_arr_item_id");
        arrangementSearchesListResponse.setArrangementElements(List.of(arrangementItem));

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(
            Mono.just(arrangementSearchesListResponse));

        StepVerifier.create(arrangementService.getArrangementByExternalId(externalId))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(response.getId(), arrangementItem.getId());
            }).verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void getArrangementByExternalId_NotFound() {
        String externalId = "external_id";

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));
        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.getArrangementByExternalId(externalId)).verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void getArrangementInternalId() {
        String externalId = "external_id";

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));

        ArrangementItem arrangementItem = new ArrangementItem();
        arrangementItem.setId("internal_id");

        ArrangementSearchesListResponse arrangementSearchesListResponse = new ArrangementSearchesListResponse();
        arrangementSearchesListResponse.setArrangementElements(List.of(arrangementItem));

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(
            Mono.just(arrangementSearchesListResponse));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId))
            .assertNext(response -> Assertions.assertEquals(response, arrangementItem.getId()))
            .verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void getArrangementInternalId_NotFound() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND,
            "Arrangement Not Found");

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(
            Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId)).verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void getArrangementInternalId_Failure() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request to get Internal Id");

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(
            Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId)).verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void deleteArrangementByInternalId() {
        String arrangementInternalId = "arr_internal_id";

        ArrangementItem arrangementItem = new ArrangementItem();
        arrangementItem.setExternalArrangementId("ext_arr_id");

        ArrangementsDeleteItem arrangementsDeleteItem = new ArrangementsDeleteItem();
        arrangementsDeleteItem.setSelector(SelectorEnum.EXTERNAL_ID);
        arrangementsDeleteItem.setValue("ext_arr_id");

        Set<ArrangementsDeleteItem> arrangementsDeleteItemSet = Set.of(arrangementsDeleteItem);
        ArrangementsDeleteResponseElement arrangementsDeleteResponseElement = new ArrangementsDeleteResponseElement();
        arrangementsDeleteResponseElement.setValue(arrangementItem.getExternalArrangementId());
        arrangementsDeleteResponseElement.setSelector(ArrangementsDeleteResponseElement.SelectorEnum.EXTERNAL_ID);

        when(arrangementsApi.getArrangementById(arrangementInternalId, false)).thenReturn(Mono.just(arrangementItem));
        when(arrangementsApi.postDelete(arrangementsDeleteItemSet)).thenReturn(
            Flux.just(arrangementsDeleteResponseElement));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .expectNext(arrangementInternalId).verifyComplete();

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi).postDelete(arrangementsDeleteItemSet);
    }

    @Test
    void deleteArrangementByInternalId_GetArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false)).thenReturn(
            Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .expectNext(arrangementInternalId).verifyComplete();

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi, times(0)).postDelete(anySet());
    }

    @Test
    void deleteArrangementByInternalId_DeleteArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        ArrangementItem accountArrangementItem = new ArrangementItem().externalArrangementId("ext_arr_id");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false)).thenReturn(
            Mono.just(accountArrangementItem));
        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        ArrangementsDeleteItem arrangementsDeleteItem = new ArrangementsDeleteItem();
        arrangementsDeleteItem.setSelector(SelectorEnum.EXTERNAL_ID);
        arrangementsDeleteItem.setValue(accountArrangementItem.getExternalArrangementId());

        Set<ArrangementsDeleteItem> arrangementsDeleteItemSet = Set.of(arrangementsDeleteItem);
        when(arrangementsApi.postDelete(arrangementsDeleteItemSet)).thenReturn(Flux.error(webClientResponseException));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(WebClientResponseException.class, e);
                Assertions.assertEquals("500 Some error", e.getMessage());
            }).verify();

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi).postDelete(arrangementsDeleteItemSet);
    }

    @Test
    void addLegalEntitiesForArrangement() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntitiesExternalIds = Set.of("leid_1", "leid_2");

        LegalEntitiesListPost legalEntitiesListPost = new LegalEntitiesListPost()
            .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
            .legalEntities(Set.of(
               new LegalEntityExternal()
                    .externalId("leid_1"),
                new LegalEntityExternal()
                    .externalId("leid_2")
            ));

        when(arrangementsIntegrationApi.postArrangementLegalEntities(legalEntitiesListPost))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                arrangementService.addLegalEntitiesForArrangement(arrangementExternalId, legalEntitiesExternalIds))
            .verifyComplete();

        verify(arrangementsIntegrationApi).postArrangementLegalEntities(legalEntitiesListPost);
    }

    @Test
    void addLegalEntitiesForArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntitiesExternalIds = Set.of("leid_1", "leid_2");

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        LegalEntitiesListPost legalEntitiesListPost = new LegalEntitiesListPost()
            .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
            .legalEntities(Set.of(
                new LegalEntityExternal()
                    .externalId("leid_1"),
                new LegalEntityExternal()
                    .externalId("leid_2")
            ));

        when(arrangementsIntegrationApi.postArrangementLegalEntities(legalEntitiesListPost))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(
                arrangementService.addLegalEntitiesForArrangement(arrangementExternalId, legalEntitiesExternalIds))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(WebClientResponseException.class, e);
                Assertions.assertEquals("500 Some error", e.getMessage());
            }).verify();

        verify(arrangementsIntegrationApi).postArrangementLegalEntities(legalEntitiesListPost);
    }

    @Test
    @Disabled
    void removeLegalEntityFromArrangement() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntityExternalIds = Set.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);
        when(arrangementsApi.getApiClient()).thenReturn(apiClient);

        LegalEntitiesDelete legalEntitiesDelete = new LegalEntitiesDelete()
            .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
            .legalEntities(legalEntityExternalIds.stream()
                .map(legalEntityExternalId -> new LegalEntityIdentification().externalId(legalEntityExternalId))
                .collect(toSet()));

        verify(arrangementsIntegrationApi).deleteArrangementLegalEntities(legalEntitiesDelete);

        StepVerifier.create(arrangementService.removeLegalEntityFromArrangement(arrangementExternalId,
            legalEntityExternalIds.stream().toList())).verifyComplete();

        verify(arrangementsIntegrationApi).getApiClient();
    }

    @Test
    @Disabled
    void removeLegalEntityFromArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntityExternalIds = Set.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);

        when(arrangementsApi.getApiClient()).thenReturn(apiClient);

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        LegalEntitiesDelete legalEntitiesDelete = new LegalEntitiesDelete()
            .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
            .legalEntities(legalEntityExternalIds.stream()
                .map(legalEntityExternalId -> new LegalEntityIdentification().externalId(legalEntityExternalId))
                .collect(toSet()));

        verify(arrangementsIntegrationApi).deleteArrangementLegalEntities(legalEntitiesDelete)
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.removeLegalEntityFromArrangement(arrangementExternalId,
                new ArrayList<>(legalEntityExternalIds)))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(WebClientResponseException.class, e);
                Assertions.assertEquals("500 Some error", e.getMessage());
            }).verify();

        verify(arrangementsApi).getApiClient();
    }

    @Test
    void addSubscriptionForArrangement() {
        // given
        var arrangementExternalId = "arr_ext_id";
        var identifiers = List.of("subsc_1", "subsc_2");
        when(arrangementsIntegrationApi.postSubscription(any())).thenReturn(Mono.empty());

        // when
        var monoResult = arrangementService
            .addSubscriptionForArrangement(arrangementExternalId, identifiers);

        // then
        StepVerifier.create(monoResult).verifyComplete();
        verify(arrangementsIntegrationApi)
            .postSubscription(new SubscriptionPost()
                .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
                .identifier("subsc_1"));
        verify(arrangementsIntegrationApi)
            .postSubscription(new SubscriptionPost()
                .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
                .identifier("subsc_2"));
    }

    @Test
    void addSubscriptionForArrangement_Failure() {
        // given
        var arrangementExternalId = "arr_ext_id";
        var identifiers = List.of("subsc_1", "subsc_2");
        var exception = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error occurred");
        when(arrangementsIntegrationApi.postSubscription(any())).thenReturn(Mono.error(exception));

        // when
        var monoResult = arrangementService
            .addSubscriptionForArrangement(arrangementExternalId, identifiers);

        // then
        StepVerifier.create(monoResult).verifyComplete();
        verify(arrangementsIntegrationApi)
            .postSubscription(new SubscriptionPost()
                .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
                .identifier("subsc_1"));
        verify(arrangementsIntegrationApi)
            .postSubscription(new SubscriptionPost()
                .arrangement(new ArrangementIdentification().externalId(arrangementExternalId))
                .identifier("subsc_2"));
    }

}
