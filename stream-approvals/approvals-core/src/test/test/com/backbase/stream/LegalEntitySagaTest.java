package com.backbase.stream;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.configuration.LegalEntitySagaConfigurationProperties;
import com.backbase.stream.legalentity.model.BaseProductGroup;
import com.backbase.stream.legalentity.model.BusinessFunctionGroup;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.JobRole;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.product.BatchProductIngestionSaga;
import com.backbase.stream.product.task.ProductGroupTask;
import com.backbase.stream.service.AccessGroupService;
import com.backbase.stream.service.LegalEntityService;
import com.backbase.stream.service.UserProfileService;
import com.backbase.stream.service.UserService;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LegalEntitySagaTest {

    @InjectMocks
    private LegalEntitySaga legalEntitySaga;

    @Mock
    private LegalEntityService legalEntityService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private AccessGroupService accessGroupService;

    @Mock
    private BatchProductIngestionSaga batchProductIngestionSaga;

    @Mock
    private LegalEntitySagaConfigurationProperties legalEntitySagaConfigurationProperties;


    @Test
    void customServiceAgreementCreation() {
        String leExternalId = "someLeExternalId";
        String adminExId = "someAdminExId";
        String regularUserExId = "someRegularUserExId";
        String customSaExId = "someCustomSaExId";

        SavingsAccount account = new SavingsAccount();
        account.externalId("someAccountExId").productTypeExternalId("Account").currency("GBP");
        ProductGroup productGroup = new ProductGroup();
        productGroup.productGroupType(BaseProductGroup.ProductGroupTypeEnum.ARRANGEMENTS).name("somePgName")
            .description("somePgDescription").savingAccounts(Collections.singletonList(account));

        ProductGroupTask productGroupTask = new ProductGroupTask(productGroup);
        Mono<ProductGroupTask> productGroupTaskMono = Mono.just(productGroupTask);

        JobProfileUser regularUser = new JobProfileUser().user(new User().internalId("someRegularUserInId")
            .externalId(regularUserExId));
        BusinessFunctionGroup functionGroup = new BusinessFunctionGroup().name("someFunctionGroup");
        JobRole jobRole = new JobRole().functionGroups(Collections.singletonList(functionGroup)).name("someJobRole");
        ServiceAgreement customSa = new ServiceAgreement().externalId(customSaExId).addJobRolesItem(jobRole);
        User adminUser = new User().internalId("someAdminInId").externalId(adminExId);
        LegalEntity legalEntity = new LegalEntity().externalId(leExternalId).addAdministratorsItem(adminUser)
            .customServiceAgreement(customSa).users(Collections.singletonList(regularUser))
            .productGroups(Collections.singletonList(productGroup));

        LegalEntityTask task = mockLegalEntityTask(legalEntity);

        when(legalEntityService.getLegalEntityByExternalId(eq(leExternalId))).thenReturn(Mono.empty());
        when(legalEntityService.createLegalEntity(any())).thenReturn(Mono.just(legalEntity));
        when(accessGroupService.getServiceAgreementByExternalId(eq(customSaExId))).thenReturn(Mono.empty());
        when(accessGroupService.createServiceAgreement(any(), eq(customSa))).thenReturn(Mono.just(customSa));
        when(accessGroupService.setupJobRole(any(), any(), any())).thenReturn(Mono.just(jobRole));
        when(accessGroupService.updateServiceAgreementRegularUsers(eq(task), eq(customSa), any()))
            .thenReturn(Mono.just(customSa));

        when(userService.getUserByExternalId(eq(regularUserExId))).thenReturn(Mono.just(regularUser.getUser()));
        when(userService.getUserByExternalId(eq(adminExId))).thenReturn(Mono.just(adminUser));
        when(userService.createUser(any(), any())).thenReturn(Mono.empty());
        when(batchProductIngestionSaga.process(any(ProductGroupTask.class))).thenReturn(productGroupTaskMono);


        Mono<LegalEntityTask> result = legalEntitySaga.executeTask(task);
        result.block();


        verify(accessGroupService).createServiceAgreement(eq(task), eq(customSa));
        verify(accessGroupService).updateServiceAgreementRegularUsers(eq(task), eq(customSa), any());
        verify(accessGroupService).setupJobRole(eq(task), eq(customSa), eq(jobRole));
    }

    @NotNull
    private LegalEntityTask mockLegalEntityTask(LegalEntity legalEntity) {
        LegalEntityTask task = Mockito.mock(LegalEntityTask.class);
        when(task.getData()).thenReturn(legalEntity);
        when(task.data(any())).thenReturn(task);
        when(task.addHistory(any())).thenReturn(task);
        return task;
    }
}