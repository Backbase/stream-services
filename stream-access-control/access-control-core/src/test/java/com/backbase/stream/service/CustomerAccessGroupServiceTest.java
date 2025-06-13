package com.backbase.stream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupUserPermissionItem;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.GetCustomerAccessGroups;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.IdItem;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.worker.exception.StreamTaskException;
import com.backbase.stream.worker.model.StreamTask;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomerAccessGroupServiceTest {

    @InjectMocks
    private CustomerAccessGroupService cagService;

    @Mock
    private CustomerAccessGroupApi customerAccessGroupApi;

    @Test
    void testCreateCustomerAccessGroup() {
        long cagId = 1L;
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);
        CustomerAccessGroup cag = new CustomerAccessGroup().name(saName).description(saDesc)
            .mandatory(true);

        when(customerAccessGroupApi.createCustomerAccessGroup(any())).thenReturn(Mono.just(new IdItem().id(cagId)));

        CustomerAccessGroupItem actual = cagService.createCustomerAccessGroup(streamTask, cag).block();

        assertEquals(cagId, actual.getId());

        verify(customerAccessGroupApi).createCustomerAccessGroup(cag);
    }

    @Test
    void testHandleErrorOnCreateCustomerAccessGroup() {
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);
        CustomerAccessGroup cag = new CustomerAccessGroup().name(saName).description(saDesc)
            .mandatory(true);

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(customerAccessGroupApi.createCustomerAccessGroup(any()))
            .thenReturn(Mono.error(error));

        assertThrows(StreamTaskException.class,
            () -> cagService.createCustomerAccessGroup(streamTask, cag).block());

        verify(streamTask).error("customer-access-group", "create", "failed", saName, "", error, errorMessage,
            "Failed to create Customer Access Group");
    }

    @Test
    void testGetCustomerAccessGroupById() {
        long cagId = 1L;
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);
        CustomerAccessGroup cag = new CustomerAccessGroup().name(saName).description(saDesc)
            .mandatory(true);

        when(customerAccessGroupApi.getCustomerAccessGroupById(any())).thenReturn(Mono.just(cag));

        CustomerAccessGroupItem actual = cagService.getCustomerAccessGroup(streamTask, cagId).block();

        CustomerAccessGroupItem expected = new CustomerAccessGroupItem()
            .id(cagId).name(saName).description(saDesc).mandatory(true);
        assertEquals(expected, actual);
        verify(customerAccessGroupApi).getCustomerAccessGroupById(cagId);
    }

    @Test
    void testUpdateCustomerAccessGroup() {
        long cagId = 1L;
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);
        CustomerAccessGroup cag = new CustomerAccessGroup().name(saName).description(saDesc)
            .mandatory(true);

        when(customerAccessGroupApi.updateCustomerAccessGroup(any(), any())).thenReturn(Mono.empty());

        cagService.updateCustomerAccessGroup(streamTask, cagId, cag).block();

        verify(customerAccessGroupApi).updateCustomerAccessGroup(cagId, cag);
    }

    @Test
    void testHandleErrorOnUpdateCustomerAccessGroup() {
        long cagId = 1L;
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);
        CustomerAccessGroup cag = new CustomerAccessGroup().name(saName).description(saDesc)
            .mandatory(true);

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(customerAccessGroupApi.updateCustomerAccessGroup(any(), any())).thenReturn(Mono.error(error));

        assertThrows(StreamTaskException.class,
            () -> cagService.updateCustomerAccessGroup(streamTask, cagId, cag).block());

        verify(streamTask).error("customer-access-group", "update", "failed", null, String.valueOf(cagId), error,
            errorMessage,
            "Failed to update CAG");
    }

    @Test
    void testDeleteCustomerAccessGroup() {
        long cagId = 1L;
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        when(customerAccessGroupApi.deleteCustomerAccessGroupById(any())).thenReturn(Mono.empty());

        cagService.deleteCustomerAccessGroup(streamTask, cagId).block();

        verify(customerAccessGroupApi).deleteCustomerAccessGroupById(cagId);
    }

    @Test
    void testHandleErrorOnDeleteCustomerAccessGroup() {
        long cagId = 1L;
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(customerAccessGroupApi.deleteCustomerAccessGroupById(any())).thenReturn(Mono.error(error));

        assertThrows(StreamTaskException.class,
            () -> cagService.deleteCustomerAccessGroup(streamTask, cagId).block());

        verify(streamTask).error("customer-access-group", "delete", "failed", null, String.valueOf(cagId), error,
            errorMessage,
            "Failed to delete CAG");
    }

    @Test
    void testGetCustomerAccessGroups() {
        String saName = "someSaName";
        String saDesc = "someSaDesc";
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        CustomerAccessGroupItem cag1Item = new CustomerAccessGroupItem().id(1L).name(saName).description(saDesc)
            .mandatory(true);
        CustomerAccessGroupItem cag2Item = new CustomerAccessGroupItem().id(2L).name(saName + "1")
            .description(saDesc + "1").mandatory(false);

        GetCustomerAccessGroups getCustomerAccessGroups = new GetCustomerAccessGroups()
            .customerAccessGroups(List.of(cag1Item, cag2Item))
            .totalCount(2L);

        when(customerAccessGroupApi.getCustomerAccessGroups(any(), any(), any())).thenReturn(
            Mono.just(getCustomerAccessGroups));

        List<CustomerAccessGroupItem> actual = cagService.getCustomerAccessGroups(streamTask).block();

        assertThat(actual).containsExactlyInAnyOrder(cag1Item, cag2Item);
        verify(customerAccessGroupApi).getCustomerAccessGroups(null, 100, null);
    }

    @Test
    void testAssignCustomerAccessGroupsToLegalEntity() {
        Set<Long> cagIds = Set.of(1L, 2L);
        String leInternalId = "leInternalId";
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        LegalEntityV2 legalEntity = new LegalEntityV2()
            .internalId(leInternalId);

        when(customerAccessGroupApi.assignCustomerAccessGroupsToLegalEntity(any(), any())).thenReturn(Mono.empty());

        cagService.assignCustomerAccessGroupsToLegalEntity(streamTask, legalEntity, cagIds).block();

        verify(customerAccessGroupApi).assignCustomerAccessGroupsToLegalEntity(leInternalId, cagIds);
    }

    @Test
    void testHandleErrorOnAssignCustomerAccessGroupsToLegalEntity() {
        Set<Long> cagIds = Set.of(1L, 2L);
        String leInternalId = "leInternalId";
        String leExternalId = "leExternalId";
        StreamTask streamTask = Mockito.mock(StreamTask.class);

        LegalEntityV2 legalEntity = new LegalEntityV2()
            .internalId(leInternalId)
            .externalId(leExternalId);

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(customerAccessGroupApi.assignCustomerAccessGroupsToLegalEntity(any(), any())).thenReturn(
            Mono.error(error));

        assertThrows(StreamTaskException.class,
            () -> cagService.assignCustomerAccessGroupsToLegalEntity(streamTask, legalEntity, cagIds).block());

        verify(streamTask).error("customer-access-group", "assign", "failed", legalEntity.getExternalId(), leInternalId,
            error, errorMessage, "Failed to assign CAGs to Legal Entity");
    }

    @Test
    void testAssignCustomerAccessGroupsToJobRole() {
        Set<Long> cagIds = Set.of(1L, 2L);
        String functionGroupId = "functionGroupId";
        String serviceAgreementId = "serviceAgreementId";
        String serviceAgreementExtId = "serviceAgreementExtId";

        String userId = "userId";
        User user = new User().internalId(userId);
        JobProfileUser jobProfileUser = new JobProfileUser().user(user);

        StreamTask streamTask = Mockito.mock(StreamTask.class);

        List<CustomerAccessGroupUserPermissionItem> userPermissionItems = List.of(
            new CustomerAccessGroupUserPermissionItem().customerAccessGroupIds(cagIds).functionGroupId(functionGroupId)
        );

        ServiceAgreementV2 serviceAgreement = new ServiceAgreementV2()
            .internalId(serviceAgreementId)
            .externalId(serviceAgreementExtId)
            .jobRoles(List.of(new JobRole().id(functionGroupId)));

        when(customerAccessGroupApi.assignCustomerAccessGroupsToJobRoles(any(), any(), any())).thenReturn(Mono.empty());

        cagService.assignCustomerAccessGroupsToJobRoles(streamTask, jobProfileUser, serviceAgreement,
            Map.of(functionGroupId, cagIds)).block();

        verify(customerAccessGroupApi).assignCustomerAccessGroupsToJobRoles(userId, serviceAgreementId,
            userPermissionItems);
    }

    @Test
    void testHandleErrorOnAssignCustomerAccessGroupsToJobRole() {
        Set<Long> cagIds = Set.of(1L);
        String functionGroupId = "functionGroupId";
        String serviceAgreementId = "serviceAgreementId";
        String serviceAgreementExtId = "serviceAgreementExtId";
        String userId = "userId";
        User user = new User().internalId(userId);
        JobProfileUser jobProfileUser = new JobProfileUser().user(user);

        StreamTask streamTask = Mockito.mock(StreamTask.class);

        ServiceAgreementV2 serviceAgreement = new ServiceAgreementV2()
            .internalId(serviceAgreementId)
            .externalId(serviceAgreementExtId)
            .jobRoles(List.of(new JobRole().id(functionGroupId)));

        String errorMessage = "some error";
        WebClientResponseException.InternalServerError error =
            Mockito.mock(WebClientResponseException.InternalServerError.class);
        when(error.getResponseBodyAsString()).thenReturn(errorMessage);
        when(customerAccessGroupApi.assignCustomerAccessGroupsToJobRoles(any(), any(), any())).thenReturn(
            Mono.error(error));

        assertThrows(StreamTaskException.class,
            () -> cagService.assignCustomerAccessGroupsToJobRoles(streamTask, jobProfileUser, serviceAgreement,
                Map.of(functionGroupId, cagIds)).block());

        verify(streamTask).error("customer-access-group", "assign", "failed", serviceAgreementExtId,
            serviceAgreementId, error, errorMessage, "Failed to assign CAGs to Function Group");
    }

}
