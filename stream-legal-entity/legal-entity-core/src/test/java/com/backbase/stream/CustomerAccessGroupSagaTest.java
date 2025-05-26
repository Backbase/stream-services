package com.backbase.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.stream.configuration.CustomerAccessGroupConfigurationProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.service.CustomerAccessGroupService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerAccessGroupSagaTest {

    @InjectMocks
    private CustomerAccessGroupSaga customerAccessGroupSaga;

    @Mock
    private CustomerAccessGroupService customerAccessGroupService;

    @Spy
    private final CustomerAccessGroupConfigurationProperties customerAccessGroupConfigurationProperties =
        new CustomerAccessGroupConfigurationProperties(true);

    @Test
    void shouldCreateCustomerAccessGroup() {
        CustomerAccessGroupItem customerAccessGroupItem = new CustomerAccessGroupItem();
        customerAccessGroupItem.name("cag-name");
        customerAccessGroupItem.description("cag-description");
        customerAccessGroupItem.setMandatory(true);
        CustomerAccessGroupTask task = mockCustomerAccessGroupTask(customerAccessGroupItem);

        when(customerAccessGroupService.createCustomerAccessGroup(any(), any()))
            .thenReturn(Mono.just(customerAccessGroupItem));

        Mono<CustomerAccessGroupTask> result = customerAccessGroupSaga.executeTask(task);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(customerAccessGroupItem, result.block().getCustomerAccessGroup());

        CustomerAccessGroup customerAccessGroup = new CustomerAccessGroup();
        customerAccessGroup.setName(customerAccessGroupItem.getName());
        customerAccessGroup.setDescription(customerAccessGroupItem.getDescription());
        customerAccessGroup.setMandatory(customerAccessGroupItem.getMandatory());

        verify(customerAccessGroupService).createCustomerAccessGroup(eq(task), eq(customerAccessGroup));
    }

    @Test
    void shouldAssignCustomerAccessGroupsToLegalEntity() {
        List<String> cagNames = List.of("cag-name-1", "cag-name-2");
        String leInternalId = "someLegalEntityId";
        String leExternalId = "someCustomerAccessGroupId";
        LegalEntityV2 legalEntityV2 = new LegalEntityV2()
            .internalId(leInternalId).externalId(leExternalId)
            .customerAccessGroupNames(cagNames);
        LegalEntityTaskV2 task = mockLegalEntityTask(legalEntityV2);

        CustomerAccessGroupItem cag1 = new CustomerAccessGroupItem()
            .id(1L)
            .name("cag-name-1")
            .description("cag-description-1")
            .mandatory(true);

        CustomerAccessGroupItem cag2 = new CustomerAccessGroupItem()
            .id(2L)
            .name("cag-name-2")
            .description("cag-description-2")
            .mandatory(false);

        when(customerAccessGroupService.getCustomerAccessGroups(any())).thenReturn(Mono.just(List.of(cag1, cag2)));
        when(customerAccessGroupService.assignCustomerAccessGroupsToLegalEntity(any(), any(), any())).thenReturn(
            Mono.just(legalEntityV2)
        );

        Mono<LegalEntityTaskV2> result = customerAccessGroupSaga.assignCustomerAccessGroupsToLegalEntity(task);
        Assertions.assertNotNull(result);

        verify(customerAccessGroupService).getCustomerAccessGroups(eq(task));
        verify(customerAccessGroupService).assignCustomerAccessGroupsToLegalEntity(eq(task), eq(legalEntityV2),
            eq(Set.of(1L, 2L)));
    }

    @Test
    void shouldAssignCustomerAccessGroupsToJobRole() {
        List<String> cagNames = List.of("cag-name-1", "cag-name-2");
        String functionGroup = "functionGroupId";
        String userId = "userId";

        User user = new User().internalId(userId);
        BusinessFunctionGroup businessFunctionGroup = new BusinessFunctionGroup().id(functionGroup);
        JobProfileUser jobProfileUser = new JobProfileUser()
            .user(user)
            .businessFunctionGroups(List.of(businessFunctionGroup))
            .customerAccessGroupNames(cagNames);

        ServiceAgreementV2 serviceAgreementV2 = new ServiceAgreementV2()
            .jobProfileUsers(List.of(jobProfileUser));
        ServiceAgreementTaskV2 task = mockServiceAgreementTask(serviceAgreementV2);

        CustomerAccessGroupItem cag1 = new CustomerAccessGroupItem()
            .id(1L)
            .name("cag-name-1")
            .description("cag-description-1")
            .mandatory(true);

        CustomerAccessGroupItem cag2 = new CustomerAccessGroupItem()
            .id(2L)
            .name("cag-name-2")
            .description("cag-description-2")
            .mandatory(false);

        when(customerAccessGroupService.getCustomerAccessGroups(any())).thenReturn(Mono.just(List.of(cag1, cag2)));
        when(customerAccessGroupService.assignCustomerAccessGroupsToJobRoles(any(), any(), any(), any())).thenReturn(
            Mono.just(serviceAgreementV2)
        );

        Map<BusinessFunctionGroup, List<BaseProductGroup>> baseProductGroupMap = new HashMap<>();
        baseProductGroupMap.put(businessFunctionGroup, List.of(new BaseProductGroup().internalId("dataGroupId")));

        Map<User, Map<BusinessFunctionGroup, List<BaseProductGroup>>> map = Map.of(user, baseProductGroupMap);

        Mono<ServiceAgreementTaskV2> result = customerAccessGroupSaga.assignCustomerAccessGroupsToJobRoles(task, map);
        Assertions.assertNotNull(result);

        verify(customerAccessGroupService).getCustomerAccessGroups(eq(task));
        verify(customerAccessGroupService).assignCustomerAccessGroupsToJobRoles(eq(task), eq(userId),
            eq(serviceAgreementV2), eq(Map.of(functionGroup, Set.of(1L, 2L))));
    }


    private CustomerAccessGroupTask mockCustomerAccessGroupTask(CustomerAccessGroupItem customerAccessGroupItem) {
        CustomerAccessGroupTask task = Mockito.mock(CustomerAccessGroupTask.class);
        when(task.getCustomerAccessGroup()).thenReturn(customerAccessGroupItem);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }

    private LegalEntityTaskV2 mockLegalEntityTask(LegalEntityV2 legalEntity) {
        LegalEntityTaskV2 task = Mockito.mock(LegalEntityTaskV2.class);
        when(task.getData()).thenReturn(legalEntity);
        when(task.data(any())).thenReturn(task);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }

    private ServiceAgreementTaskV2 mockServiceAgreementTask(ServiceAgreementV2 serviceAgreement) {
        ServiceAgreementTaskV2 task = Mockito.mock(ServiceAgreementTaskV2.class);
        when(task.getServiceAgreement()).thenReturn(serviceAgreement);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }
}
