package com.backbase.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroup;
import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.dbs.accesscontrol.api.service.v3.FunctionGroupsApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.stream.configuration.CustomerAccessGroupConfigurationProperties;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRoleNameToCustomerAccessGroupNames;
import com.backbase.stream.legalentity.model.LegalEntityV2;
import com.backbase.stream.legalentity.model.ServiceAgreementV2;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.service.CustomerAccessGroupService;
import com.backbase.stream.worker.exception.StreamTaskException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerAccessGroupSagaTest {

    @InjectMocks
    private CustomerAccessGroupSaga customerAccessGroupSaga;

    @Mock
    private CustomerAccessGroupService customerAccessGroupService;

    @Mock
    private FunctionGroupsApi functionGroupsApi;

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

        when(customerAccessGroupService.getCustomerAccessGroups()).thenReturn(Mono.just(Collections.emptyList()));

        Mono<CustomerAccessGroupTask> result = customerAccessGroupSaga.executeTask(task);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(customerAccessGroupItem, result.block().getCustomerAccessGroup());

        CustomerAccessGroup customerAccessGroup = new CustomerAccessGroup();
        customerAccessGroup.setName(customerAccessGroupItem.getName());
        customerAccessGroup.setDescription(customerAccessGroupItem.getDescription());
        customerAccessGroup.setMandatory(customerAccessGroupItem.getMandatory());

        verify(customerAccessGroupService).createCustomerAccessGroup(task, customerAccessGroup);
    }

    @Test
    void shouldUpdateCreatedCustomerAccessGroup() {
        CustomerAccessGroupItem customerAccessGroupItem = new CustomerAccessGroupItem();
        customerAccessGroupItem.name("cag-name");
        customerAccessGroupItem.description("cag-description");
        customerAccessGroupItem.setMandatory(true);
        customerAccessGroupItem.setId(1L);
        CustomerAccessGroupTask task = mockCustomerAccessGroupTask(customerAccessGroupItem);

        when(customerAccessGroupService.updateCustomerAccessGroup(any(), any(), any()))
            .thenReturn(Mono.empty());

        when(customerAccessGroupService.getCustomerAccessGroups())
            .thenReturn(Mono.just(List.of(customerAccessGroupItem)));

        Mono<CustomerAccessGroupTask> result = customerAccessGroupSaga.executeTask(task);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(customerAccessGroupItem, result.block().getCustomerAccessGroup());

        CustomerAccessGroup customerAccessGroup = new CustomerAccessGroup();
        customerAccessGroup.setName(customerAccessGroupItem.getName());
        customerAccessGroup.setDescription(customerAccessGroupItem.getDescription());
        customerAccessGroup.setMandatory(customerAccessGroupItem.getMandatory());

        verify(customerAccessGroupService).updateCustomerAccessGroup(task, 1L, customerAccessGroup);
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

        when(customerAccessGroupService.getCustomerAccessGroups()).thenReturn(Mono.just(List.of(cag1, cag2)));
        when(customerAccessGroupService.assignCustomerAccessGroupsToLegalEntity(any(), any(), any())).thenReturn(
            Mono.just(legalEntityV2)
        );

        Mono<LegalEntityTaskV2> result = customerAccessGroupSaga.assignCustomerAccessGroupsToLegalEntity(task);
        StepVerifier.create(result).expectNext(task).verifyComplete();

        verify(customerAccessGroupService).getCustomerAccessGroups();
        verify(customerAccessGroupService).assignCustomerAccessGroupsToLegalEntity(task, legalEntityV2, Set.of(1L, 2L));
    }

    @Test
    void shouldThrowExceptionWhenAssigningCagToLegalEntityIfCagNameNotExist() {
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
        when(customerAccessGroupService.getCustomerAccessGroups()).thenReturn(Mono.just(List.of(cag1)));
        when(customerAccessGroupService.assignCustomerAccessGroupsToLegalEntity(any(), any(), any())).thenReturn(
            Mono.just(legalEntityV2)
        );

        StepVerifier.create(customerAccessGroupSaga.assignCustomerAccessGroupsToLegalEntity(task))
            .expectErrorMessage("Customer access groups '[cag-name-2]' do not exist").verify();
    }

    @Test
    void shouldAssignCustomerAccessGroupsToJobRole() {
        String fg1Name = "fg1Name";
        String fg2Name = "fg2Name";
        List<String> cagNames = List.of("cag-name-1", "cag-name-2");
        String functionGroup1 = "functionGroup1Id";
        String functionGroup2 = "functionGroup2Id";
        String userId = "userId";

        User user = new User().internalId(userId);
        BusinessFunctionGroup businessFunctionGroup1 = new BusinessFunctionGroup().id(functionGroup1);
        BusinessFunctionGroup businessFunctionGroup2 = new BusinessFunctionGroup().id(functionGroup2);
        JobProfileUser jobProfileUser = new JobProfileUser()
            .user(user)
            .businessFunctionGroups(List.of(businessFunctionGroup1, businessFunctionGroup2))
            .jobRoleNameToCustomerAccessGroupNames(List.of(
                new JobRoleNameToCustomerAccessGroupNames().jobRoleName(fg1Name).customerAccessGroupNames(cagNames),
                new JobRoleNameToCustomerAccessGroupNames().jobRoleName(fg2Name)
            ));

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

        when(customerAccessGroupService.getCustomerAccessGroups()).thenReturn(Mono.just(List.of(cag1, cag2)));
        when(customerAccessGroupService.assignCustomerAccessGroupsToJobRoles(any(), any(), any(), any())).thenReturn(
            Mono.just(jobProfileUser)
        );
        FunctionGroupItem functionGroupItem1 = new FunctionGroupItem().id(functionGroup1).name(fg1Name);
        FunctionGroupItem functionGroupItem2 = new FunctionGroupItem().id(functionGroup2).name(fg2Name);

        when(functionGroupsApi.getFunctionGroups(any())).thenReturn(Flux.just(functionGroupItem1, functionGroupItem2));

        Mono<ServiceAgreementTaskV2> result = customerAccessGroupSaga.assignCustomerAccessGroupsToJobRoles(task);
        StepVerifier.create(result).expectNext(task).verifyComplete();

        verify(customerAccessGroupService).getCustomerAccessGroups();
        verify(customerAccessGroupService).assignCustomerAccessGroupsToJobRoles(task, jobProfileUser,
            serviceAgreementV2, Map.of(functionGroup1, Set.of(1L, 2L), functionGroup2, Collections.emptySet()));
    }

    @Test
    void shouldThrowExceptionIfFunctionGroupNameNotExist() {
        String fg1Name = "fg1Name";
        String fg2Name = "fg2Name";
        List<String> cagNames = List.of("cag-name-1", "cag-name-2");
        String functionGroup1 = "functionGroup1Id";
        String functionGroup2 = "functionGroup2Id";
        String userId = "userId";

        User user = new User().internalId(userId);
        BusinessFunctionGroup businessFunctionGroup1 = new BusinessFunctionGroup().id(functionGroup1);
        BusinessFunctionGroup businessFunctionGroup2 = new BusinessFunctionGroup().id(functionGroup2);
        JobProfileUser jobProfileUser = new JobProfileUser()
            .user(user)
            .businessFunctionGroups(List.of(businessFunctionGroup1, businessFunctionGroup2))
            .jobRoleNameToCustomerAccessGroupNames(List.of(
                new JobRoleNameToCustomerAccessGroupNames().jobRoleName("RandomName").customerAccessGroupNames(cagNames),
                new JobRoleNameToCustomerAccessGroupNames().jobRoleName(fg2Name)
            ));

        ServiceAgreementV2 serviceAgreementV2 = new ServiceAgreementV2()
            .jobProfileUsers(List.of(jobProfileUser));
        ServiceAgreementTaskV2 task = mockServiceAgreementTask(serviceAgreementV2);

        FunctionGroupItem functionGroupItem1 = new FunctionGroupItem().id(functionGroup1).name(fg1Name);
        FunctionGroupItem functionGroupItem2 = new FunctionGroupItem().id(functionGroup2).name(fg2Name);

        when(functionGroupsApi.getFunctionGroups(any())).thenReturn(Flux.just(functionGroupItem1, functionGroupItem2));

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
        when(customerAccessGroupService.getCustomerAccessGroups()).thenReturn(Mono.just(List.of(cag1, cag2)));

        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> customerAccessGroupSaga.assignCustomerAccessGroupsToJobRoles(task).block());

        assertThat(exception.getMessage()).isEqualTo("Function group with name 'RandomName' does not exist");
    }

    @Test
    void shouldThrowExceptionIfCustomerAccessGroupNameNotExist() {
        String fg1Name = "fg1Name";
        String fg2Name = "fg2Name";
        List<String> cagNames = List.of("cag-name-1", "cag-name-2");
        String functionGroup1 = "functionGroup1Id";
        String functionGroup2 = "functionGroup2Id";
        String userId = "userId";

        User user = new User().internalId(userId);
        BusinessFunctionGroup businessFunctionGroup1 = new BusinessFunctionGroup().id(functionGroup1);
        BusinessFunctionGroup businessFunctionGroup2 = new BusinessFunctionGroup().id(functionGroup2);
        JobProfileUser jobProfileUser = new JobProfileUser()
            .user(user)
            .businessFunctionGroups(List.of(businessFunctionGroup1, businessFunctionGroup2))
            .jobRoleNameToCustomerAccessGroupNames(List.of(
                new JobRoleNameToCustomerAccessGroupNames().jobRoleName(fg1Name).customerAccessGroupNames(cagNames),
                new JobRoleNameToCustomerAccessGroupNames().jobRoleName(fg2Name)
            ));

        ServiceAgreementV2 serviceAgreementV2 = new ServiceAgreementV2()
            .jobProfileUsers(List.of(jobProfileUser));
        ServiceAgreementTaskV2 task = mockServiceAgreementTask(serviceAgreementV2);

        CustomerAccessGroupItem cag1 = new CustomerAccessGroupItem()
            .id(1L)
            .name("cag-name-1")
            .description("cag-description-1")
            .mandatory(true);

        when(customerAccessGroupService.getCustomerAccessGroups()).thenReturn(Mono.just(List.of(cag1)));
        when(customerAccessGroupService.assignCustomerAccessGroupsToJobRoles(any(), any(), any(), any())).thenReturn(
            Mono.just(jobProfileUser)
        );
        FunctionGroupItem functionGroupItem1 = new FunctionGroupItem().id(functionGroup1).name(fg1Name);
        FunctionGroupItem functionGroupItem2 = new FunctionGroupItem().id(functionGroup2).name(fg2Name);

        when(functionGroupsApi.getFunctionGroups(any())).thenReturn(Flux.just(functionGroupItem1, functionGroupItem2));

        StreamTaskException exception = assertThrows(StreamTaskException.class,
            () -> customerAccessGroupSaga.assignCustomerAccessGroupsToJobRoles(task).block());

        assertThat(exception.getMessage()).isEqualTo("Customer access groups '[cag-name-2]' do not exist");
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
