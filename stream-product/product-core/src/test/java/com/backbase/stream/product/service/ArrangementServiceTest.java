package com.backbase.stream.product.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.integration.v2.model.ArrangementAddedResponse;
import com.backbase.dbs.arrangement.api.integration.v2.model.BatchResponseItemExtended;
import com.backbase.dbs.arrangement.api.integration.v2.model.BatchResponseStatusCode;
import com.backbase.dbs.arrangement.api.integration.v2.model.ErrorItem;
import com.backbase.dbs.arrangement.api.integration.v2.model.ExternalLegalEntityIds;
import com.backbase.dbs.arrangement.api.integration.v2.model.PostArrangement;
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
import java.util.Set;
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
    private com.backbase.dbs.arrangement.api.integration.v2.ArrangementsApi arrangementsIntegrationApi;

    private static WebClientResponseException buildWebClientResponseException(HttpStatus httpStatus, String statusText) {
        return WebClientResponseException.create(httpStatus.value(), statusText, null, null, null);
    }

    private static PostArrangement buildPostArrangement() {
        PostArrangement postArrangement = new PostArrangement();
        postArrangement.setId("ext_arr_id");
        postArrangement.setLegalEntityIds(Set.of("ext_leid_1", "ext_leid_2"));
        postArrangement.setProductId("ext_prod_id");
        postArrangement.setStateId("ext_state_id");
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
        PostArrangement request = buildPostArrangement();

        ArrangementItem accountArrangementAddedResponse = new ArrangementItem().id("arr_response_id");

        ArrangementAddedResponse arrangementAddedResponse = new ArrangementAddedResponse().id(accountArrangementAddedResponse.getId());
        when(arrangementsIntegrationApi.postArrangements(request)).thenReturn(Mono.just(arrangementAddedResponse));

        StepVerifier.create(arrangementService.createArrangement(request))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(accountArrangementAddedResponse.getId(), response.getId());
                Assertions.assertEquals(request.getProductId(), response.getProductId());
                Assertions.assertNotNull(response.getState());
                Assertions.assertEquals(request.getStateId(), response.getState().getState());
                Assertions.assertEquals(request.getProductId(), response.getProductId());
                Assertions.assertEquals(request.getLegalEntityIds(), response.getLegalEntityIds());
            }).verifyComplete();

        verify(arrangementsIntegrationApi).postArrangements(request);
    }

    @Test
    void createArrangement_Failure() {
        PostArrangement request = buildPostArrangement();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for create arrangement");

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
        ArrangementPutItem request = buildArrangementPutItem();

        when(arrangementsApi.putArrangementById(request.getExternalArrangementId(), request)).thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.updateArrangement(request))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(request.getExternalArrangementId(), response.getExternalArrangementId());
                Assertions.assertEquals(request.getProductId(), response.getProductId());
            }).verifyComplete();

        verify(arrangementsApi).putArrangementById(request.getExternalArrangementId(), request);
    }

    @Test
    void updateArrangement_Failure() {
        ArrangementPutItem request = buildArrangementPutItem();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for update arrangement");
        when(arrangementsApi.putArrangementById(request.getExternalArrangementId(), request)).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.updateArrangement(request))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(ArrangementUpdateException.class, e);
                Assertions.assertEquals("Failed to update Arrangement: %s".formatted(request.getExternalArrangementId()), e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            }).verify();

        verify(arrangementsApi).putArrangementById(request.getExternalArrangementId(), request);
    }

    @Test
    void upsertBatchArrangements() {
        PostArrangement request = buildPostArrangement();

        BatchResponseItemExtended accountBatchResponseItemExtended = (BatchResponseItemExtended) new BatchResponseItemExtended()
            .arrangementId("arr_id")
            .resourceId("resource_id")
            .status(BatchResponseStatusCode.HTTP_STATUS_OK);

        when(arrangementsIntegrationApi.postBatchUpsertArrangements(any())).thenReturn(Flux.just(accountBatchResponseItemExtended));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(accountBatchResponseItemExtended.getArrangementId(),
                    response.getArrangementId());
            }).verifyComplete();

        verify(arrangementsIntegrationApi).postBatchUpsertArrangements(any());
    }

    @Test
    void upsertBatchArrangements_Batch_Error() {
        PostArrangement request = buildPostArrangement();

        BatchResponseItemExtended accountBatchResponseItemExtended = new BatchResponseItemExtended();
        accountBatchResponseItemExtended.setArrangementId("arr_id");
        accountBatchResponseItemExtended.setResourceId("resource_id");
        accountBatchResponseItemExtended.setStatus(BatchResponseStatusCode.HTTP_STATUS_BAD_REQUEST);
        accountBatchResponseItemExtended.addErrorsItem(new ErrorItem().message("Some error"));
        accountBatchResponseItemExtended.addErrorsItem(new ErrorItem().message("Some other error"));

        when(arrangementsIntegrationApi.postBatchUpsertArrangements(any())).thenReturn(Flux.just(accountBatchResponseItemExtended));

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
        PostArrangement request = buildPostArrangement();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for upsert arrangement");
        when(arrangementsIntegrationApi.postBatchUpsertArrangements(any())).thenReturn(Flux.error(webClientResponseException));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(ArrangementUpdateException.class, e);
                Assertions.assertEquals("Batch arrangement update failed: " + List.of(request), e.getMessage());
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

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND, "Arrangement Not Found");
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

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(Mono.just(arrangementSearchesListResponse));

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

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(Mono.just(arrangementSearchesListResponse));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId))
            .assertNext(response -> Assertions.assertEquals(response, arrangementItem.getId()))
            .verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void getArrangementInternalId_NotFound() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND, "Arrangement Not Found");

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId)).verifyComplete();

        verify(arrangementsApi).postSearchArrangements(arrangementsSearchesPostRequest);
    }

    @Test
    void getArrangementInternalId_Failure() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request to get Internal Id");

        ArrangementsSearchesPostRequest arrangementsSearchesPostRequest = new ArrangementsSearchesPostRequest();
        arrangementsSearchesPostRequest.setExternalArrangementIds(Set.of(externalId));

        when(arrangementsApi.postSearchArrangements(arrangementsSearchesPostRequest)).thenReturn(Mono.error(webClientResponseException));

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
        when(arrangementsApi.postDelete(arrangementsDeleteItemSet)).thenReturn(Flux.just(arrangementsDeleteResponseElement));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .expectNext(arrangementInternalId).verifyComplete();

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi).postDelete(arrangementsDeleteItemSet);
    }

    @Test
    void deleteArrangementByInternalId_GetArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false)).thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .expectNext(arrangementInternalId).verifyComplete();

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi, times(0)).postDelete(anySet());
    }

    @Test
    void deleteArrangementByInternalId_DeleteArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        ArrangementItem accountArrangementItem = new ArrangementItem().externalArrangementId("ext_arr_id");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false)).thenReturn(Mono.just(accountArrangementItem));
        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

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

        ExternalLegalEntityIds externalLegalEntityIds = new ExternalLegalEntityIds();
        externalLegalEntityIds.ids(legalEntitiesExternalIds);

        when(arrangementsIntegrationApi.postArrangementLegalEntities(arrangementExternalId, externalLegalEntityIds))
            .thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.addLegalEntitiesForArrangement(arrangementExternalId, new ArrayList<>(legalEntitiesExternalIds)))
            .verifyComplete();

        verify(arrangementsIntegrationApi).postArrangementLegalEntities(arrangementExternalId, externalLegalEntityIds);
    }

    @Test
    void addLegalEntitiesForArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntitiesExternalIds = Set.of("leid_1", "leid_2");

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        ExternalLegalEntityIds externalLegalEntityIds = new ExternalLegalEntityIds();
        externalLegalEntityIds.ids(legalEntitiesExternalIds);

        when(arrangementsIntegrationApi.postArrangementLegalEntities(arrangementExternalId, externalLegalEntityIds))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.addLegalEntitiesForArrangement(arrangementExternalId,
            new ArrayList<>(legalEntitiesExternalIds))).consumeErrorWith(e -> {
            Assertions.assertInstanceOf(WebClientResponseException.class, e);
            Assertions.assertEquals("500 Some error", e.getMessage());
        }).verify();

        verify(arrangementsIntegrationApi).postArrangementLegalEntities(arrangementExternalId, externalLegalEntityIds);
    }

    @Test
    @Disabled
    void removeLegalEntityFromArrangement() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntityExternalIds = Set.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);
        when(arrangementsApi.getApiClient()).thenReturn(apiClient);

        ExternalLegalEntityIds externalLegalEntityIds = new ExternalLegalEntityIds();
        externalLegalEntityIds.setIds(legalEntityExternalIds);

        verify(arrangementsIntegrationApi).deleteArrangementLegalEntities(arrangementExternalId, externalLegalEntityIds);

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

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        ExternalLegalEntityIds externalLegalEntityIds = new ExternalLegalEntityIds();
        externalLegalEntityIds.setIds(legalEntityExternalIds);

        verify(arrangementsIntegrationApi).deleteArrangementLegalEntities(arrangementExternalId, externalLegalEntityIds)
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.removeLegalEntityFromArrangement(arrangementExternalId, new ArrayList<>(legalEntityExternalIds)))
            .consumeErrorWith(e -> {
                Assertions.assertInstanceOf(WebClientResponseException.class, e);
                Assertions.assertEquals("500 Some error", e.getMessage());
            }).verify();

        verify(arrangementsApi).getApiClient();
    }

}
