package com.backbase.stream.product.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementAddedResponse;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPost;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItems;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountBatchResponseItemExtended;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountExternalLegalEntityIds;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountInternalIdGetResponseBody;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountUserPreferencesItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.BatchResponseStatusCode;
import com.backbase.dbs.arrangement.api.service.v2.model.ErrorItem;
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
public class ArrangementServiceTest {

    @InjectMocks
    private ArrangementService arrangementService;

    @Mock
    private ArrangementsApi arrangementsApi;

    private static WebClientResponseException buildWebClientResponseException(HttpStatus httpStatus,
        String statusText) {
        return WebClientResponseException.create(httpStatus.value(), statusText, null, null, null);
    }

    private static AccountArrangementItemPost buildAccountArrangementItemPost() {
        return new AccountArrangementItemPost()
            .externalArrangementId("ext_arr_id")
            .externalLegalEntityIds(Set.of("ext_leid_1", "ext_leid_2"))
            .legalEntityIds(Set.of("leid_1", "leid_2"))
            .externalProductId("ext_prod_id")
            .externalStateId("ext_state_id")
            .productId("prod_id");
    }

    private static AccountArrangementItemPut buildAccountArrangementItemPut() {
        return new AccountArrangementItemPut()
            .externalArrangementId("ext_arr_id")
            .productId("prod_id");
    }

    private static AccountUserPreferencesItemPut buildAccountUserPreferencesItemPut() {
        return new AccountUserPreferencesItemPut()
            .arrangementId("arr_id")
            .userId("user_id");
    }

    @Test
    void createArrangement() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        AccountArrangementAddedResponse accountArrangementAddedResponse = new AccountArrangementAddedResponse().id(
            "arr_response_id");
        when(arrangementsApi.postArrangements(any()))
            .thenReturn(Mono.just(accountArrangementAddedResponse));

        StepVerifier.create(arrangementService.createArrangement(request))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(accountArrangementAddedResponse.getId(), response.getId());
                Assertions.assertEquals(request.getExternalArrangementId(), response.getExternalArrangementId());
                Assertions.assertEquals(request.getExternalProductId(), response.getExternalProductId());
                Assertions.assertEquals(request.getExternalStateId(), response.getExternalStateId());
                Assertions.assertEquals(request.getExternalProductId(), response.getExternalProductId());
                Assertions.assertEquals(request.getLegalEntityIds(), response.getLegalEntityIds());
            })
            .verifyComplete();

        verify(arrangementsApi).postArrangements(any());
    }

    @Test
    void createArrangement_Failure() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request for create arrangement");

        when(arrangementsApi.postArrangements(any()))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.createArrangement(request))
            .consumeErrorWith(e -> {
                Assertions.assertTrue(e instanceof ArrangementCreationException);
                Assertions.assertEquals("Failed to post arrangements", e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            })
            .verify();

        verify(arrangementsApi).postArrangements(any());
    }

    @Test
    void updateArrangement() {
        AccountArrangementItemPut request = buildAccountArrangementItemPut();

        when(arrangementsApi.putArrangements(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.updateArrangement(request))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(request.getExternalArrangementId(), response.getExternalArrangementId());
                Assertions.assertEquals(request.getProductId(), response.getProductId());
            })
            .verifyComplete();

        verify(arrangementsApi).putArrangements(any());
    }

    @Test
    void updateArrangement_Failure() {
        AccountArrangementItemPut request = buildAccountArrangementItemPut();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request for update arrangement");
        when(arrangementsApi.putArrangements(any()))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.updateArrangement(request))
            .consumeErrorWith(e -> {
                Assertions.assertTrue(e instanceof ArrangementUpdateException);
                Assertions.assertEquals("Failed to update Arrangement: " + request.getExternalArrangementId(),
                    e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            })
            .verify();

        verify(arrangementsApi).putArrangements(any());
    }

    @Test
    void upsertBatchArrangements() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        AccountBatchResponseItemExtended accountBatchResponseItemExtended = new AccountBatchResponseItemExtended()
            .arrangementId("arr_id")
            .resourceId("resource_id")
            .status(BatchResponseStatusCode.HTTP_STATUS_OK);

        when(arrangementsApi.postBatchUpsertArrangements(any()))
            .thenReturn(Flux.just(accountBatchResponseItemExtended));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(accountBatchResponseItemExtended.getArrangementId(),
                    response.getArrangementId());
            })
            .verifyComplete();

        verify(arrangementsApi).postBatchUpsertArrangements(any());
    }

    @Test
    void upsertBatchArrangements_Batch_Error() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        AccountBatchResponseItemExtended accountBatchResponseItemExtended = new AccountBatchResponseItemExtended()
            .arrangementId("arr_id")
            .resourceId("resource_id")
            .status(BatchResponseStatusCode.HTTP_STATUS_BAD_REQUEST)
            .addErrorsItem(new ErrorItem().message("Some error"))
            .addErrorsItem(new ErrorItem().message("Some other error"));

        when(arrangementsApi.postBatchUpsertArrangements(any()))
            .thenReturn(Flux.just(accountBatchResponseItemExtended));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .consumeErrorWith(e -> {
                String errorMessage = e.getMessage();
                Assertions.assertTrue(errorMessage.startsWith("Batch arrangement update failed: 'resource_id'"));
                Assertions.assertTrue(errorMessage.contains("message: Some error"));
                Assertions.assertTrue(errorMessage.contains("message: Some other error"));
            })
            .verify();

        verify(arrangementsApi).postBatchUpsertArrangements(any());
    }

    @Test
    void upsertBatchArrangements_Failure() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request for upsert arrangement");
        when(arrangementsApi.postBatchUpsertArrangements(any()))
            .thenReturn(Flux.error(webClientResponseException));

        StepVerifier.create(arrangementService.upsertBatchArrangements(List.of(request)))
            .consumeErrorWith(e -> {
                Assertions.assertTrue(e instanceof ArrangementUpdateException);
                Assertions.assertEquals("Batch arrangement update failed: " + List.of(request), e.getMessage());
                Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());
            })
            .verify();

        verify(arrangementsApi).postBatchUpsertArrangements(any());
    }

    @Test
    void updateUserPreferences() {
        AccountUserPreferencesItemPut request = buildAccountUserPreferencesItemPut();

        when(arrangementsApi.putUserPreferences(any()))
            .thenReturn(Mono.empty());

        // arrangementService.updateUserPreferences(request).block();

        StepVerifier.create(arrangementService.updateUserPreferences(request))
            .verifyComplete();

        verify(arrangementsApi).putUserPreferences(any());
    }

    @Test
    void updateUserPreferences_NotFound() {
        AccountUserPreferencesItemPut request = buildAccountUserPreferencesItemPut();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND,
            "User Not Found");
        when(arrangementsApi.putUserPreferences(any()))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.updateUserPreferences(request))
            .verifyComplete();

        verify(arrangementsApi).putUserPreferences(any());
    }

    @Test
    void getArrangement() {
        String internalId = "internal_id";
        AccountArrangementItem accountArrangementItem = new AccountArrangementItem().id("acct_arr_item_id");

        when(arrangementsApi.getArrangementById(internalId, false))
            .thenReturn(Mono.just(accountArrangementItem));

        StepVerifier.create(arrangementService.getArrangement(internalId))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(response.getId(), accountArrangementItem.getId());
            })
            .verifyComplete();

        verify(arrangementsApi).getArrangementById(internalId, false);
    }

    @Test
    void getArrangement_NotFound() {
        String internalId = "internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND,
            "Arrangement Not Found");
        when(arrangementsApi.getArrangementById(internalId, false))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangement(internalId))
            .verifyComplete();

        verify(arrangementsApi).getArrangementById(internalId, false);
    }

    @Test
    void getArrangementByExternalId() {
        String externalId = "external_id";

        AccountArrangementItem accountArrangementItem = new AccountArrangementItem().id("acct_arr_item_id");
        AccountArrangementItems accountArrangementItems = new AccountArrangementItems()
            .addArrangementElementsItem(accountArrangementItem);
        when(arrangementsApi.getArrangements(null, null, List.of(externalId)))
            .thenReturn(Mono.just(accountArrangementItems));

        StepVerifier.create(arrangementService.getArrangementByExternalId(externalId))
            .assertNext(response -> {
                Assertions.assertNotNull(response);
                Assertions.assertEquals(response.getId(), accountArrangementItem.getId());
            })
            .verifyComplete();

        verify(arrangementsApi).getArrangements(null, null, List.of(externalId));
    }

    @Test
    void getArrangementByExternalId_NotFound() {
        String externalId = "external_id";

        when(arrangementsApi.getArrangements(null, null, List.of(externalId)))
            .thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.getArrangementByExternalId(externalId))
            .verifyComplete();

        verify(arrangementsApi).getArrangements(null, null, List.of(externalId));
    }

    @Test
    void getArrangementInternalId() {
        String externalId = "external_id";
        AccountInternalIdGetResponseBody accountInternalIdGetResponseBody =
            new AccountInternalIdGetResponseBody().internalId("internal_id");

        when(arrangementsApi.getInternalId(externalId))
            .thenReturn(Mono.just(accountInternalIdGetResponseBody));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId))
            .assertNext(response -> Assertions.assertEquals(response, accountInternalIdGetResponseBody.getInternalId()))
            .verifyComplete();

        verify(arrangementsApi).getInternalId(externalId);
    }

    @Test
    void getArrangementInternalId_NotFound() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND,
            "Arrangement Not Found");
        when(arrangementsApi.getInternalId(externalId))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId))
            .verifyComplete();

        verify(arrangementsApi).getInternalId(externalId);
    }

    @Test
    void getArrangementInternalId_Failure() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST,
            "Bad Request to get Internal Id");
        when(arrangementsApi.getInternalId(externalId))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.getArrangementInternalId(externalId))
            .verifyComplete();

        verify(arrangementsApi).getInternalId(externalId);
    }

    @Test
    void deleteArrangementByInternalId() {
        String arrangementInternalId = "arr_internal_id";
        AccountArrangementItem accountArrangementItem = new AccountArrangementItem()
            .externalArrangementId("ext_arr_id");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false))
            .thenReturn(Mono.just(accountArrangementItem));

        when(arrangementsApi.deleteExternalArrangementId(accountArrangementItem.getExternalArrangementId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .expectNext(arrangementInternalId)
            .verifyComplete();

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi).deleteExternalArrangementId(accountArrangementItem.getExternalArrangementId());
    }

    @Test
    void deleteArrangementByInternalId_GetArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .expectNext(arrangementInternalId)
            .verifyComplete();

        verify(arrangementsApi, times(1)).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi, times(0)).deleteExternalArrangementId(anyString());
    }

    @Test
    void deleteArrangementByInternalId_DeleteArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        AccountArrangementItem accountArrangementItem = new AccountArrangementItem()
            .externalArrangementId("ext_arr_id");
        when(arrangementsApi.getArrangementById(arrangementInternalId, false))
            .thenReturn(Mono.just(accountArrangementItem));

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");
        when(arrangementsApi.deleteExternalArrangementId(accountArrangementItem.getExternalArrangementId()))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.deleteArrangementByInternalId(arrangementInternalId))
            .consumeErrorWith(e -> {
                Assertions.assertTrue(e instanceof WebClientResponseException);
                Assertions.assertEquals("500 Some error", e.getMessage());
            })
            .verify();

        verify(arrangementsApi, times(1)).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi, times(1)).deleteExternalArrangementId(anyString());
    }

    @Test
    void addLegalEntitiesForArrangement() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntitiesExternalIds = Set.of("leid_1", "leid_2");
        AccountExternalLegalEntityIds accountExternalLegalEntityIds =
            new AccountExternalLegalEntityIds().ids(legalEntitiesExternalIds);

        when(arrangementsApi.postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds))
            .thenReturn(Mono.empty());

        StepVerifier.create(arrangementService.addLegalEntitiesForArrangement(arrangementExternalId,
                new ArrayList<>(legalEntitiesExternalIds)))
            .verifyComplete();

        verify(arrangementsApi).postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds);
    }

    @Test
    void addLegalEntitiesForArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntitiesExternalIds = Set.of("leid_1", "leid_2");
        AccountExternalLegalEntityIds accountExternalLegalEntityIds =
            new AccountExternalLegalEntityIds().ids(legalEntitiesExternalIds);

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");
        when(arrangementsApi.postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(arrangementService.addLegalEntitiesForArrangement(arrangementExternalId,
                new ArrayList<>(legalEntitiesExternalIds)))
            .consumeErrorWith(e -> {
                Assertions.assertTrue(e instanceof WebClientResponseException);
                Assertions.assertEquals("500 Some error", e.getMessage());
            })
            .verify();

        verify(arrangementsApi).postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds);
    }

    @Test
    @Disabled
    void removeLegalEntityFromArrangement() {
        String arrangementExternalId = "arr_ext_id";
        List<String> legalEntityExternalIds = List.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);
        when(arrangementsApi.getApiClient())
            .thenReturn(apiClient);

        verify(arrangementsApi).deleteArrangementLegalEntities(eq(arrangementExternalId),
            argThat(accountExternalLegalEntityIds -> accountExternalLegalEntityIds.getIds()
                    .containsAll(legalEntityExternalIds)));

        StepVerifier.create(arrangementService.removeLegalEntityFromArrangement(arrangementExternalId,
                legalEntityExternalIds))
            .verifyComplete();

        verify(arrangementsApi).getApiClient();
    }

    @Test
    @Disabled
    void removeLegalEntityFromArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        Set<String> legalEntityExternalIds = Set.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);

        when(arrangementsApi.getApiClient())
            .thenReturn(apiClient);

        WebClientResponseException webClientResponseException = buildWebClientResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        verify(arrangementsApi).deleteArrangementLegalEntities(eq(arrangementExternalId),
            argThat(accountExternalLegalEntityIds -> accountExternalLegalEntityIds.getIds()
                .containsAll(legalEntityExternalIds)))
            .thenReturn(Mono.error(webClientResponseException));

        StepVerifier.create(
                arrangementService.removeLegalEntityFromArrangement(arrangementExternalId,
                    new ArrayList<>(legalEntityExternalIds)))
            .consumeErrorWith(e -> {
                Assertions.assertTrue(e instanceof WebClientResponseException);
                Assertions.assertEquals("500 Some error", e.getMessage());
            })
            .verify();

        verify(arrangementsApi).getApiClient();
    }

}
