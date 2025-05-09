package com.backbase.stream.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.loan.inbound.api.service.v2.LoansApi;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.loan.LoansSaga;
import com.backbase.stream.product.configuration.ProductIngestionSagaConfigurationProperties;
import com.backbase.stream.product.service.ArrangementService;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductIngestionSagaTest {

  @InjectMocks
  ProductIngestionSaga productIngestionSaga;
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

  ProductGroupTask productGroupTask;

  @BeforeEach
  void setUp() {
    productGroupTask = mockProductGroupTask();
    when(arrangementService.getArrangementInternalId(anyString()))
        .thenReturn(Mono.just("internal_id"));
    when(arrangementService.createArrangement(any()))
        .thenReturn(Mono.just(new ArrangementItem()));
    when(arrangementService.updateArrangement(anyString(), any()))
        .thenReturn(Mono.just(new ArrangementPutItem()
            .externalArrangementId("currentAccountExtId")));
    when(accessGroupService.setupProductGroups(productGroupTask))
        .thenReturn(Mono.just(productGroupTask));
    when(accessGroupService.getFunctionGroupsForServiceAgreement("sa_internalId"))
        .thenReturn(Mono.just(List.of(MockUtil.buildFunctionGroupItem())));
    when(accessGroupService.setupFunctionGroups(productGroupTask,
        productGroupTask.getData().getServiceAgreement(),
        MockUtil.buildBusinessFunctionGroupList()))
        .thenReturn(Mono.just(MockUtil.buildBusinessFunctionGroupList()));
    when(accessGroupService.assignPermissions(any(), any())).thenReturn(
        Mono.just(MockUtil.buildJobProfile()));
  }

  @Test
  void test_processProductsUsers_withUserInternalId() {
    when(userService.getUserById("someRegularUserInId")).thenReturn(
        Mono.just(MockUtil.buildUser()));
    when(userService.createUser(any(), any(), any()))
        .thenReturn(Mono.just(MockUtil.buildUser()));
    StepVerifier.create(productIngestionSaga.process(productGroupTask))
        .expectNext(productGroupTask)
        .expectComplete()
        .verify();
  }

  @Test
  void test_processProductsUsers_withUserExternalId() {
    productGroupTask.getProductGroup().getUsers().get(0).getUser().setInternalId(null);
    when(userService.getUserByExternalId("someRegularUserExId")).thenReturn(
        Mono.just(MockUtil.buildUser()));
    when(userService.createUser(any(), any(), any()))
        .thenReturn(Mono.just(MockUtil.buildUser()));
    StepVerifier.create(productIngestionSaga.process(productGroupTask))
        .expectNext(productGroupTask)
        .expectComplete()
        .verify();
  }

  ProductGroupTask mockProductGroupTask() {
    ProductGroup productGroup = MockUtil.createProductGroup();
    return new ProductGroupTask()
        .data(productGroup);
  }
}