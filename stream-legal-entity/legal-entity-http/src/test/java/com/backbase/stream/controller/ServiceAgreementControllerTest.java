package com.backbase.stream.controller;

import static com.backbase.stream.legalentity.model.UpdatedServiceAgreementResponse.StateEnum.ACCEPTED;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.CustomerAccessGroupApi;
import com.backbase.customerprofile.api.integration.v1.PartyManagementIntegrationApi;
import com.backbase.dbs.accesscontrol.api.service.v3.LegalEntitiesApi;
import com.backbase.dbs.accesscontrol.api.service.v3.model.FunctionGroupItem;
import com.backbase.dbs.arrangement.api.service.v3.ArrangementsApi;
import com.backbase.dbs.contact.api.service.v2.ContactsApi;
import com.backbase.dbs.limit.api.service.v2.LimitsServiceApi;
import com.backbase.dbs.user.api.service.v2.IdentityManagementApi;
import com.backbase.dbs.user.api.service.v2.UserManagementApi;
import com.backbase.dbs.user.api.service.v2.UserProfileManagementApi;
import com.backbase.dbs.user.api.service.v2.model.GetUser;
import com.backbase.loan.inbound.api.service.v2.LoansApi;
import com.backbase.stream.CustomerAccessGroupSaga;
import com.backbase.stream.audiences.UserKindSegmentationSaga;
import com.backbase.stream.clients.config.CustomerProfileClientConfig;
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
import com.backbase.stream.mapper.PartyMapper;
import com.backbase.stream.product.task.BatchProductGroupTask;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.streams.tailoredvalue.PlansService;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

//@ExtendWith(SpringExtension.class)
//@WebFluxTest(ServiceAgreementController.class)
//@AutoConfigureWebTestClient
//@TestPropertySource(properties = {"spring.cloud.kubernetes.enabled=false", "spring.cloud.config.enabled=false"})
//@Import({LegalEntityHttpConfiguration.class, LegalEntitySagaConfiguration.class,
//    UpdatedServiceAgreementSagaConfiguration.class})
class ServiceAgreementControllerTest {


}
