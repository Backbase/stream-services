package com.backbase.stream.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountBatchResponseItemExtended;
import com.backbase.dbs.arrangement.api.service.v2.model.BatchResponseStatusCode;
import com.backbase.loan.inbound.api.service.v1.LoansApi;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.loan.LoansSaga;
import com.backbase.stream.loan.LoansTask;
import com.backbase.stream.product.configuration.ProductIngestionSagaConfigurationProperties;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

  BatchProductGroupTask batchProductGroupTask;

  @BeforeEach
  void setUp() {
    batchProductGroupTask = mockBatchProductGroupTask();
    when(arrangementService.upsertBatchArrangements(anyList()))
        .thenReturn(Flux.just(new AccountBatchResponseItemExtended()
            .arrangementId("arr_id")
            .resourceId("resource_id")
            .status(BatchResponseStatusCode.HTTP_STATUS_OK)));
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

  BatchProductGroupTask mockBatchProductGroupTask() {
    ProductGroup productGroup = MockUtil.createProductGroup();
    return new BatchProductGroupTask()
        .data(new BatchProductGroup().productGroups(List.of(productGroup))
            .serviceAgreement(productGroup.getServiceAgreement()));

  }
}
