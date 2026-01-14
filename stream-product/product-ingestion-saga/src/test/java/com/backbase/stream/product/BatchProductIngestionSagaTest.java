package com.backbase.stream.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.integration.v3.model.BatchResponseItem;
import com.backbase.dbs.arrangement.api.integration.v3.model.BatchResponseStatusCode;
import com.backbase.loan.inbound.api.service.v2.LoansApi;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.IdentityUserLinkStrategy;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.loan.LoansSaga;
import com.backbase.stream.loan.LoansTask;
import com.backbase.stream.product.configuration.ProductIngestionSagaConfigurationProperties;
import com.backbase.stream.product.mapping.ProductMapper;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BatchProductIngestionSagaTest {

    @InjectMocks
    BatchProductIngestionSaga batchProductIngestionSaga;
    @Mock
    ArrangementService arrangementService;
    @Mock
    AccessGroupService accessGroupService;
    @Mock
    UserService userService;
    @Mock
    ProductIngestionSagaConfigurationProperties configurationProperties;
    @Mock
    LoansSaga loansSaga;
    @Mock
    LoansApi loansApi;
    @Mock
    ProductMapper productMapper;

    BatchProductGroupTask batchProductGroupTask;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(batchProductIngestionSaga, "productMapper", Mappers.getMapper(ProductMapper.class));
        batchProductGroupTask = mockBatchProductGroupTask();
        BatchResponseItem batchResponseItemExtended = new BatchResponseItem();
        batchResponseItemExtended.setArrangementExternalId("resource_id");
        batchResponseItemExtended.setStatus(BatchResponseStatusCode.HTTP_STATUS_OK);
        when(arrangementService.upsertBatchArrangements(anyList()))
            .thenReturn(Flux.just(batchResponseItemExtended
                .arrangementId("arr_id")));
        when(accessGroupService.getExistingDataGroups(
            batchProductGroupTask.getData().getServiceAgreement()
                .getInternalId(), null))
            .thenReturn(Flux.just(MockUtil.buildDataGroupItem()));
        when(accessGroupService.createArrangementDataAccessGroup(any(), any(), any()))
            .thenReturn(Mono.just(new BaseProductGroup()));
        when(accessGroupService.updateExistingDataGroupsBatch(batchProductGroupTask,
            List.of(MockUtil.buildDataGroupItem()),
            batchProductGroupTask.getData().getProductGroups()))
            .thenReturn(Mono.just(batchProductGroupTask));
        when(accessGroupService.getFunctionGroupsForServiceAgreement("sa_internalId"))
            .thenReturn(Mono.just(List.of(MockUtil.buildFunctionGroupItem())));
        when(accessGroupService.setupFunctionGroups(batchProductGroupTask,
            batchProductGroupTask.getData().getServiceAgreement(),
            MockUtil.buildBusinessFunctionGroupList()))
            .thenReturn(Mono.just(MockUtil.buildBusinessFunctionGroupList()));
        when(accessGroupService.assignPermissionsBatch(any(), any())).thenReturn(
            Mono.just(batchProductGroupTask));
        when(arrangementService.addSubscriptionForArrangement(any(), any())).thenReturn(
            Mono.empty()
        );
        LoansTask loansTask = new LoansTask(
            String.format(batchProductGroupTask.getData().getServiceAgreement().getExternalId(),
                batchProductGroupTask.getId()), List.of(MockUtil.buildLoanAccount()));
        when(loansSaga.executeTask(loansTask)).thenReturn(Mono.just(loansTask));
    }

    @Test
    void test_processProductBatchUsers_withUserInternalId() {
        when(userService.getUserById("someRegularUserInId")).thenReturn(
            Mono.just(MockUtil.buildUser()));
        when(userService.createUser(any(), any(), any()))
            .thenReturn(Mono.just(MockUtil.buildUser()));
        StepVerifier.create(batchProductIngestionSaga.process(batchProductGroupTask))
            .expectNext(batchProductGroupTask)
            .expectComplete()
            .verify();
    }

    @Test
    void test_processProductBatchUsers_withUserExternalId() {
        batchProductGroupTask.getBatchProductGroup().getProductGroups().get(0).getUsers().get(0)
            .getUser().setInternalId(null);
        when(userService.getUserByExternalId("someRegularUserExId")).thenReturn(
            Mono.just(MockUtil.buildUser()));
        when(userService.createUser(any(), any(), any()))
            .thenReturn(Mono.just(MockUtil.buildUser()));
        StepVerifier.create(batchProductIngestionSaga.process(batchProductGroupTask))
            .expectNext(batchProductGroupTask)
            .expectComplete()
            .verify();
    }

    @Test
    void test_processProductBatchExistingIdentityUser_withUserInternalId() {
        when(configurationProperties.isIdentityEnabled()).thenReturn(true);
        User user = MockUtil.buildUser();
        batchProductGroupTask.getBatchProductGroup().getProductGroups().get(0).getUsers()
            .get(0).getUser().setIdentityLinkStrategy(IdentityUserLinkStrategy.CREATE_IN_IDENTITY);
        batchProductGroupTask.getBatchProductGroup().getProductGroups().get(0).getUsers()
            .get(0).getUser().setInternalId("someRegularUserInId");
        when(userService.getUserById("someRegularUserInId")).thenReturn(
            Mono.just(user));
        when(userService.createOrImportIdentityUser(any(), any(), any()))
            .thenReturn(Mono.empty());
        StepVerifier.create(batchProductIngestionSaga.process(batchProductGroupTask))
            .expectNext(batchProductGroupTask)
            .expectComplete()
            .verify();
    }

    @Test
    void test_processProductBatchNewIdentityUser_withUserInternalId() {
        when(configurationProperties.isIdentityEnabled()).thenReturn(true);
        User user = MockUtil.buildUser();
        batchProductGroupTask.getBatchProductGroup().getProductGroups().get(0).getUsers()
            .get(0).getUser().setIdentityLinkStrategy(IdentityUserLinkStrategy.CREATE_IN_IDENTITY);
        batchProductGroupTask.getBatchProductGroup().getProductGroups().get(0).getUsers()
            .get(0).getUser().setInternalId("someRegularUserInId");
        when(userService.getUserById("someRegularUserInId")).thenReturn(
            Mono.just(user));
        when(userService.createOrImportIdentityUser(any(), any(), any()))
            .thenReturn(Mono.just(user));
        StepVerifier.create(batchProductIngestionSaga.process(batchProductGroupTask))
            .expectNext(batchProductGroupTask)
            .expectComplete()
            .verify();
    }

    @Test
    void test_processProductBatchIdentityUsers_withUserExternalId() {
        batchProductGroupTask.getBatchProductGroup().getProductGroups().get(0).getUsers().get(0)
            .getUser().setInternalId(null);
        when(userService.getUserByExternalId("someRegularUserExId")).thenReturn(
            Mono.just(MockUtil.buildUser()));
        when(userService.createUser(any(), any(), any()))
            .thenReturn(Mono.just(MockUtil.buildUser()));
        StepVerifier.create(batchProductIngestionSaga.process(batchProductGroupTask))
            .expectNext(batchProductGroupTask)
            .expectComplete()
            .verify();
    }

    @Test
    void test_processProductBatch_subscriptionAdding() {
        when(userService.getUserById("someRegularUserInId")).thenReturn(
            Mono.just(MockUtil.buildUser()));
        when(userService.createUser(any(), any(), any()))
            .thenReturn(Mono.just(MockUtil.buildUser()));
        StepVerifier.create(batchProductIngestionSaga.process(batchProductGroupTask))
            .expectNext(batchProductGroupTask)
            .expectComplete()
            .verify();
        verify(arrangementService, times(5)).addSubscriptionForArrangement(any(), anyList());
    }

    BatchProductGroupTask mockBatchProductGroupTask() {
        ProductGroup productGroup = MockUtil.createProductGroup();
        return new BatchProductGroupTask()
            .data(new BatchProductGroup().productGroups(List.of(productGroup))
                .serviceAgreement(productGroup.getServiceAgreement()));

    }
}