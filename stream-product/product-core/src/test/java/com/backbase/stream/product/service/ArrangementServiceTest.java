package com.backbase.stream.product.service;

import com.backbase.dbs.arrangement.api.service.ApiClient;
import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.api.service.v2.model.*;
import com.backbase.stream.product.exception.ArrangementCreationException;
import com.backbase.stream.product.exception.ArrangementUpdateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ArrangementServiceTest {

    @InjectMocks
    private ArrangementService arrangementService;

    @Mock
    private ArrangementsApi arrangementsApi;

    private static WebClientResponseException buildWebClientResponseException(HttpStatus httpStatus, String statusText) {
        return WebClientResponseException.create(httpStatus.value(), statusText, null, null, null);
    }

    private static AccountArrangementItemPost buildAccountArrangementItemPost() {
        return new AccountArrangementItemPost()
                .externalArrangementId("ext_arr_id")
                .externalLegalEntityIds(List.of("ext_leid_1", "ext_leid_2"))
                .legalEntityIds(List.of("leid_1", "leid_2"))
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

        AccountArrangementAddedResponse accountArrangementAddedResponse = new AccountArrangementAddedResponse().id("arr_response_id");
        when(arrangementsApi.postArrangements(any()))
                .thenReturn(Mono.just(accountArrangementAddedResponse));

        AccountArrangementItem response = arrangementService.createArrangement(request).block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(accountArrangementAddedResponse.getId(), response.getId());
        Assertions.assertEquals(request.getExternalArrangementId(), response.getExternalArrangementId());
        Assertions.assertEquals(request.getExternalProductId(), response.getExternalProductId());
        Assertions.assertEquals(request.getExternalStateId(), response.getExternalStateId());
        Assertions.assertEquals(request.getExternalProductId(), response.getExternalProductId());
        Assertions.assertEquals(request.getLegalEntityIds(), response.getLegalEntityIds());

        verify(arrangementsApi).postArrangements(any());
    }

    @Test
    void createArrangement_Failure() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for create arrangement");

        when(arrangementsApi.postArrangements(any()))
                .thenReturn(Mono.error(webClientResponseException));

        ArrangementCreationException e = Assertions.assertThrows(ArrangementCreationException.class,
                () -> arrangementService.createArrangement(request).block());
        Assertions.assertEquals("Failed to post arrangements", e.getMessage());
        Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());

        verify(arrangementsApi).postArrangements(any());
    }

    @Test
    void updateArrangement() {
        AccountArrangementItemPut request = buildAccountArrangementItemPut();

        when(arrangementsApi.putArrangements(any()))
                .thenReturn(Mono.empty());

        AccountArrangementItemPut response = arrangementService.updateArrangement(request).block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(request.getExternalArrangementId(), response.getExternalArrangementId());
        Assertions.assertEquals(request.getProductId(), response.getProductId());

        verify(arrangementsApi).putArrangements(any());
    }

    @Test
    void updateArrangement_Failure() {
        AccountArrangementItemPut request = buildAccountArrangementItemPut();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for update arrangement");
        when(arrangementsApi.putArrangements(any()))
                .thenReturn(Mono.error(webClientResponseException));

        ArrangementUpdateException e = Assertions.assertThrows(ArrangementUpdateException.class,
                () -> arrangementService.updateArrangement(request).block());
        Assertions.assertEquals("Failed to update Arrangement: " + request.getExternalArrangementId(), e.getMessage());
        Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());

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

        AccountBatchResponseItemExtended response = arrangementService.upsertBatchArrangements(List.of(request)).blockFirst();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(accountBatchResponseItemExtended.getArrangementId(), response.getArrangementId());

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

        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> arrangementService.upsertBatchArrangements(List.of(request)).blockFirst());
        String errorMessage = e.getMessage();
        Assertions.assertTrue(errorMessage.startsWith("Batch arrangement update failed: 'resource_id'"));
        Assertions.assertTrue(errorMessage.contains("message: Some error"));
        Assertions.assertTrue(errorMessage.contains("message: Some other error"));

        verify(arrangementsApi).postBatchUpsertArrangements(any());
    }

    @Test
    void upsertBatchArrangements_Failure() {
        AccountArrangementItemPost request = buildAccountArrangementItemPost();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request for upsert arrangement");
        when(arrangementsApi.postBatchUpsertArrangements(any()))
                .thenReturn(Flux.error(webClientResponseException));

        ArrangementUpdateException e = Assertions.assertThrows(ArrangementUpdateException.class,
                () -> arrangementService.upsertBatchArrangements(List.of(request)).blockFirst());
        Assertions.assertEquals("Batch arrangement update failed: " + List.of(request), e.getMessage());
        Assertions.assertEquals(webClientResponseException.getMessage(), e.getCause().getMessage());

        verify(arrangementsApi).postBatchUpsertArrangements(any());
    }

    @Test
    void updateUserPreferences() {
        AccountUserPreferencesItemPut request = buildAccountUserPreferencesItemPut();

        when(arrangementsApi.putUserPreferences(any()))
                .thenReturn(Mono.empty());

        arrangementService.updateUserPreferences(request).block();

        verify(arrangementsApi).putUserPreferences(any());
    }

    @Test
    void updateUserPreferences_NotFound() {
        AccountUserPreferencesItemPut request = buildAccountUserPreferencesItemPut();

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND, "User Not Found");
        when(arrangementsApi.putUserPreferences(any()))
                .thenReturn(Mono.error(webClientResponseException));

        arrangementService.updateUserPreferences(request).block();

        verify(arrangementsApi).putUserPreferences(any());
    }

    @Test
    void getArrangement() {
        String internalId = "internal_id";
        AccountArrangementItem accountArrangementItem = new AccountArrangementItem().id("acct_arr_item_id");

        when(arrangementsApi.getArrangementById(internalId, false))
                .thenReturn(Mono.just(accountArrangementItem));

        AccountArrangementItem response = arrangementService.getArrangement(internalId).block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getId(), accountArrangementItem.getId());

        verify(arrangementsApi).getArrangementById(internalId, false);
    }

    @Test
    void getArrangement_NotFound() {
        String internalId = "internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND, "Arrangement Not Found");
        when(arrangementsApi.getArrangementById(internalId, false))
                .thenReturn(Mono.error(webClientResponseException));

        arrangementService.getArrangement(internalId).block();

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

        AccountArrangementItem response = arrangementService.getArrangementByExternalId(externalId).block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getId(), accountArrangementItem.getId());

        verify(arrangementsApi).getArrangements(null, null, List.of(externalId));
    }

    @Test
    void getArrangementByExternalId_NotFound() {
        String externalId = "external_id";

        when(arrangementsApi.getArrangements(null, null, List.of(externalId)))
                .thenReturn(Mono.empty());

        arrangementService.getArrangementByExternalId(externalId).block();

        verify(arrangementsApi).getArrangements(null, null, List.of(externalId));
    }

    @Test
    void getArrangementInternalId() {
        String externalId = "external_id";
        AccountInternalIdGetResponseBody accountInternalIdGetResponseBody =
                new AccountInternalIdGetResponseBody().internalId("internal_id");

        when(arrangementsApi.getInternalId(externalId))
                .thenReturn(Mono.just(accountInternalIdGetResponseBody));

        String response = arrangementService.getArrangementInternalId(externalId).block();
        Assertions.assertEquals(response, accountInternalIdGetResponseBody.getInternalId());

        verify(arrangementsApi).getInternalId(externalId);
    }

    @Test
    void getArrangementInternalId_NotFound() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.NOT_FOUND, "Arrangement Not Found");
        when(arrangementsApi.getInternalId(externalId))
                .thenReturn(Mono.error(webClientResponseException));

        arrangementService.getArrangementInternalId(externalId).block();

        verify(arrangementsApi).getInternalId(externalId);
    }

    @Test
    void getArrangementInternalId_Failure() {
        String externalId = "external_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.BAD_REQUEST, "Bad Request to get Internal Id");
        when(arrangementsApi.getInternalId(externalId))
                .thenReturn(Mono.error(webClientResponseException));

        arrangementService.getArrangementInternalId(externalId).block();

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

        String response = arrangementService.deleteArrangementByInternalId(arrangementInternalId).block();
        Assertions.assertEquals(response, arrangementInternalId);

        verify(arrangementsApi).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi).deleteExternalArrangementId(accountArrangementItem.getExternalArrangementId());
    }

    @Test
    void deleteArrangementByInternalId_GetArrangement_Failure() {
        String arrangementInternalId = "arr_internal_id";

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");

        when(arrangementsApi.getArrangementById(arrangementInternalId, false))
                .thenReturn(Mono.error(webClientResponseException));

        arrangementService.deleteArrangementByInternalId(arrangementInternalId).block();

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

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");
        when(arrangementsApi.deleteExternalArrangementId(accountArrangementItem.getExternalArrangementId()))
                .thenReturn(Mono.error(webClientResponseException));

        WebClientResponseException e = Assertions.assertThrows(WebClientResponseException.class,
                () -> arrangementService.deleteArrangementByInternalId(arrangementInternalId).block());
        Assertions.assertEquals("500 Some error", e.getMessage());

        verify(arrangementsApi, times(1)).getArrangementById(arrangementInternalId, false);
        verify(arrangementsApi, times(1)).deleteExternalArrangementId(anyString());
    }

    @Test
    void addLegalEntitiesForArrangement() {
        String arrangementExternalId = "arr_ext_id";
        List<String> legalEntitiesExternalIds = List.of("leid_1", "leid_2");
        AccountExternalLegalEntityIds accountExternalLegalEntityIds =
                new AccountExternalLegalEntityIds().ids(legalEntitiesExternalIds);

        when(arrangementsApi.postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds))
                .thenReturn(Mono.empty());

        arrangementService.addLegalEntitiesForArrangement(arrangementExternalId, legalEntitiesExternalIds).block();

        verify(arrangementsApi).postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds);
    }

    @Test
    void addLegalEntitiesForArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        List<String> legalEntitiesExternalIds = List.of("leid_1", "leid_2");
        AccountExternalLegalEntityIds accountExternalLegalEntityIds =
                new AccountExternalLegalEntityIds().ids(legalEntitiesExternalIds);

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");
        when(arrangementsApi.postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds))
                .thenReturn(Mono.error(webClientResponseException));

        WebClientResponseException e = Assertions.assertThrows(WebClientResponseException.class,
                () -> arrangementService.addLegalEntitiesForArrangement(arrangementExternalId, legalEntitiesExternalIds).block());
        Assertions.assertEquals("500 Some error", e.getMessage());

        verify(arrangementsApi).postArrangementLegalEntities(arrangementExternalId, accountExternalLegalEntityIds);
    }

    @Test
    void removeLegalEntityFromArrangement() {
        String arrangementExternalId = "arr_ext_id";
        List<String> legalEntityExternalIds = List.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);
        when(arrangementsApi.getApiClient())
                .thenReturn(apiClient);

        when(apiClient.invokeAPI("/arrangements/{externalArrangementId}/legalentities",
                HttpMethod.DELETE,
                Collections.singletonMap("externalArrangementId", arrangementExternalId),
                new LinkedMultiValueMap<>(),
                Collections.singletonMap("ids", legalEntityExternalIds),
                new HttpHeaders(),
                new LinkedMultiValueMap<>(),
                new LinkedMultiValueMap<>(),
                apiClient.selectHeaderAccept(new String[]{"application/json"}),
                apiClient.selectHeaderContentType(new String[]{}),
                new String[]{},
                new ParameterizedTypeReference<Void>() {
                }))
                .thenReturn(Mono.empty());

        arrangementService.removeLegalEntityFromArrangement(arrangementExternalId, legalEntityExternalIds).block();

        verify(arrangementsApi).getApiClient();
    }

    @Test
    void removeLegalEntityFromArrangement_Failure() {
        String arrangementExternalId = "arr_ext_id";
        List<String> legalEntityExternalIds = List.of("leid_1", "leid_2");

        ApiClient apiClient = mock(ApiClient.class);

        when(arrangementsApi.getApiClient())
                .thenReturn(apiClient);

        WebClientResponseException webClientResponseException = buildWebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error");
        when(apiClient.invokeAPI("/arrangements/{externalArrangementId}/legalentities",
                HttpMethod.DELETE,
                Collections.singletonMap("externalArrangementId", arrangementExternalId),
                new LinkedMultiValueMap<>(),
                Collections.singletonMap("ids", legalEntityExternalIds),
                new HttpHeaders(),
                new LinkedMultiValueMap<>(),
                new LinkedMultiValueMap<>(),
                apiClient.selectHeaderAccept(new String[]{"application/json"}),
                apiClient.selectHeaderContentType(new String[]{}),
                new String[]{},
                new ParameterizedTypeReference<Void>() {
                }))
                .thenReturn(Mono.error(webClientResponseException));

        WebClientResponseException e = Assertions.assertThrows(WebClientResponseException.class,
                () -> arrangementService.removeLegalEntityFromArrangement(arrangementExternalId, legalEntityExternalIds).block());
        Assertions.assertEquals("500 Some error", e.getMessage());

        verify(arrangementsApi).getApiClient();
    }

}
