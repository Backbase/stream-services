package com.backbase.stream.controller;

import static com.backbase.stream.legalentity.model.UpdatedServiceAgreementResponse.StateEnum.ACCEPTED;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backbase.dbs.accesscontrol.api.service.v3.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.arrangement.api.service.v2.ArrangementsApi;
import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.UserProfileManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.stream.config.LegalEntityHttpConfiguration;
import com.backbase.stream.configuration.LegalEntitySagaConfiguration;
import com.backbase.stream.configuration.UpdatedServiceAgreementSagaConfiguration;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BatchProductGroup;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntityParticipant;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.ServiceAgreementUserAction;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreement;
import com.backbase.stream.legalentity.model.UpdatedServiceAgreementResponse;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(LegalEntityAsyncController.class)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@Import({
  LegalEntityHttpConfiguration.class,
  LegalEntitySagaConfiguration.class,
  UpdatedServiceAgreementSagaConfiguration.class
})
class LegalEntityAsyncControllerTest {

  @MockBean private ReactiveClientRegistrationRepository reactiveClientRegistrationRepository;

  @MockBean private WebClient webClient;

  @MockBean private com.backbase.dbs.accesscontrol.api.service.ApiClient accessControlApiClient;

  @MockBean private com.backbase.dbs.user.api.service.ApiClient userApiClient;

  @MockBean private com.backbase.dbs.user.profile.api.service.ApiClient userProfileApiClient;

  @MockBean private com.backbase.dbs.arrangement.api.service.ApiClient accountsApiClient;

  @MockBean private com.backbase.identity.integration.api.service.ApiClient identityApiClient;

  @MockBean private LimitsServiceApi limitsApi;

  @MockBean private ContactsApi contactsApi;

  @MockBean private UserManagementApi userManagementApi;

  @MockBean private AccessGroupService accessGroupService;

  @MockBean private LegalEntitiesApi legalEntitiesApi;

  @MockBean private IdentityManagementApi identityManagementApi;

  @MockBean private UserProfileManagementApi userProfileManagementApi;

  @MockBean
  private com.backbase.dbs.user.profile.api.service.v2.UserProfileManagementApi
      userProfileManagement;

  @MockBean private ArrangementsApi arrangementsApi;

  @Autowired private WebTestClient webTestClient;

  @Test
  void updateServiceAgreementAsyncTest() throws Exception {
    final String saExternalId = "someSaExternalId";
    final String saInternalId = "someSaInternalId";
    URI uri = URI.create("/async/service-agreement");
    User user1 = new User().externalId("someUserExId1");
    User user2 = new User().externalId("someUserExId2");
    LegalEntityParticipant participant =
        new LegalEntityParticipant()
            .externalId("someLeExId")
            .sharingAccounts(true)
            .sharingUsers(true);
    BaseProductGroup baseProductGroup =
        new BaseProductGroup().addLoansItem(new Loan().productNumber("1"));
    JobProfileUser jobProfileUser1 =
        new JobProfileUser().user(user1).addReferenceJobRoleNamesItem("someJobRole1");
    JobProfileUser jobProfileUser2 =
        new JobProfileUser().user(user2).addReferenceJobRoleNamesItem("someJobRole2");
    UpdatedServiceAgreement serviceAgreement =
        new UpdatedServiceAgreement()
            .addProductGroupsItem(baseProductGroup)
            .addSaUsersItem(
                new ServiceAgreementUserAction()
                    .userProfile(jobProfileUser1)
                    .action(ServiceAgreementUserAction.ActionEnum.ADD))
            .addSaUsersItem(
                new ServiceAgreementUserAction()
                    .userProfile(jobProfileUser2)
                    .action(ServiceAgreementUserAction.ActionEnum.ADD));
    serviceAgreement
        .externalId(saExternalId)
        .internalId(saInternalId)
        .name("someSa")
        .addParticipantsItem(participant);
    ServiceAgreement internalSA =
        new ServiceAgreement().externalId(saExternalId).internalId(saInternalId);
    List<FunctionGroupItem> serviceAgreementFunctionGroups =
        asList(
            new FunctionGroupItem().name("someJobRole1").type(FunctionGroupItem.TypeEnum.DEFAULT),
            new FunctionGroupItem().name("someJobRole2").type(FunctionGroupItem.TypeEnum.DEFAULT),
            new FunctionGroupItem().name("someJobRole3").type(FunctionGroupItem.TypeEnum.DEFAULT));
    ProductGroup productGroup = new ProductGroup().serviceAgreement(serviceAgreement);
    productGroup.loans(baseProductGroup.getLoans());

    when(accessGroupService.updateServiceAgreementAssociations(any(), eq(serviceAgreement), any()))
        .thenReturn(Mono.just(serviceAgreement));

    Mono<ProductGroupTask> productGroupTaskMono = Mono.just(new ProductGroupTask(productGroup));
    when(accessGroupService.setupProductGroups(any())).thenReturn(productGroupTaskMono);

    when(accessGroupService.getUserByExternalId(eq("someUserExId1"), eq(true)))
        .thenReturn(Mono.just(new GetUser().id("someUserInId1").externalId("someUserExId1")));
    when(accessGroupService.getUserByExternalId(eq("someUserExId2"), eq(true)))
        .thenReturn(Mono.just(new GetUser().id("someUserInId2").externalId("someUserExId2")));

    when(accessGroupService.getFunctionGroupsForServiceAgreement(eq("someSaInternalId")))
        .thenReturn(Mono.just(serviceAgreementFunctionGroups));

    BatchProductGroupTask bpgTask =
        new BatchProductGroupTask()
            .data(new BatchProductGroup().serviceAgreement(serviceAgreement));
    Mono<BatchProductGroupTask> bpgTaskMono = Mono.just(bpgTask);
    when(accessGroupService.assignPermissionsBatch(any(), any())).thenReturn(bpgTaskMono);

    when(accessGroupService.getServiceAgreementByExternalId(eq(saExternalId)))
        .thenReturn(Mono.just(internalSA));

    WebTestClient.ResponseSpec result =
        webTestClient
            .put()
            .uri(uri)
            .body(Mono.just(serviceAgreement), UpdatedServiceAgreement.class)
            .exchange();
    FluxExchangeResult<UpdatedServiceAgreementResponse> responseFlux =
        result.returnResult(UpdatedServiceAgreementResponse.class);
    UpdatedServiceAgreementResponse response = responseFlux.getResponseBody().blockLast();

    assertEquals(ACCEPTED, response.getState());
  }
}
