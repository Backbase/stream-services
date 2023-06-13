package com.backbase.stream.compositions.legalentity.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.stream.LegalEntitySaga;
import com.backbase.stream.LegalEntityTask;
import com.backbase.stream.compositions.legalentity.core.config.LegalEntityConfigurationProperties;
import com.backbase.stream.compositions.legalentity.core.mapper.LegalEntityMapper;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPullRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityPushRequest;
import com.backbase.stream.compositions.legalentity.core.model.LegalEntityResponse;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIngestionService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityIntegrationService;
import com.backbase.stream.compositions.legalentity.core.service.LegalEntityPostIngestionService;
import com.backbase.stream.compositions.product.client.ProductCompositionApi;
import com.backbase.stream.compositions.product.client.model.CurrentAccount;
import com.backbase.stream.compositions.product.client.model.ProductIngestionResponse;
import com.backbase.stream.legalentity.model.JobProfileUser;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.ProductGroup;
import com.backbase.stream.legalentity.model.ServiceAgreement;
import com.backbase.stream.legalentity.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LegalEntityIngestionServiceImplTest {

    private LegalEntityIngestionService legalEntityIngestionService;

    @Mock
    private LegalEntityIntegrationService legalEntityIntegrationService;

    LegalEntityMapper mapper = Mappers.getMapper(LegalEntityMapper.class);

    @Mock
    Validator validator;

    @Mock
    LegalEntitySaga legalEntitySaga;

    LegalEntityPostIngestionService legalEntityPostIngestionService;

    @Mock
    EventBus eventBus;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    LegalEntityConfigurationProperties config;

    @Mock
    ProductCompositionApi productCompositionApi;

    @Mock
    LegalEntityConfigurationProperties legalEntityConfigurationProperties;

    @BeforeEach
    void setUp() {
        legalEntityPostIngestionService = new LegalEntityPostIngestionServiceImpl(eventBus, config,
                productCompositionApi, mapper);

        legalEntityIngestionService = new LegalEntityIngestionServiceImpl(
                legalEntitySaga,
                legalEntityIntegrationService,
                validator,
                legalEntityPostIngestionService,
                legalEntityConfigurationProperties);
    }


    @Test
    @Tag("true")
    void ingestionInPullModeAsync_Success(TestInfo testInfo) {
        List<String> tags = new ArrayList<>(testInfo.getTags());
        when(productCompositionApi.pullIngestProduct(any()))
                .thenReturn(Mono.just(new ProductIngestionResponse()
                        .withProductGroups(
                                Arrays.asList((com.backbase.stream.compositions.product.client.model.ProductGroup)
                                        new com.backbase.stream.compositions.product.client.model.ProductGroup()
                                                .withCurrentAccounts(List.of(new CurrentAccount().withBBAN("test BBAN")))))));

        Mono<LegalEntityResponse> legalEntityIngestResponseMono = executeIngestionWithPullMode(
                Boolean.valueOf(tags.get(0)), Boolean.TRUE, Boolean.TRUE);
        StepVerifier.create(legalEntityIngestResponseMono)
            .assertNext(Assertions::assertNotNull).verifyComplete();
    }

    @Test
    @Tag("false")
    void ingestionInPullModeSync_Success(TestInfo testInfo) {
        List<String> tags = new ArrayList<>(testInfo.getTags());
        when(config.getChains().getIncludeSubsidiaries()).thenReturn(Boolean.TRUE);
        when(productCompositionApi.pullIngestProduct(any()))
                .thenReturn(Mono.just(new ProductIngestionResponse()
                        .withProductGroups(
                                Arrays.asList((com.backbase.stream.compositions.product.client.model.ProductGroup)
                                        new com.backbase.stream.compositions.product.client.model.ProductGroup()
                                                .withCurrentAccounts(List.of(new CurrentAccount().withBBAN("test BBAN")))))));

        Mono<LegalEntityResponse> legalEntityIngestResponseMono = executeIngestionWithPullMode(
                Boolean.valueOf(tags.get(0)), Boolean.TRUE, Boolean.TRUE);
        StepVerifier.create(legalEntityIngestResponseMono)
            .assertNext(Assertions::assertNotNull).verifyComplete();
    }

    @Test
    @Tag("false")
    void ingestionInPullModeSync_Fail(TestInfo testInfo) {
        List<String> tags = new ArrayList<>(testInfo.getTags());
        when(config.isFailedEventEnabled()).thenReturn(Boolean.TRUE);
        Mono<LegalEntityResponse> legalEntityIngestResponseMono = executeIngestionWithPullMode(
                Boolean.valueOf(tags.get(0)), Boolean.TRUE, Boolean.TRUE);
        StepVerifier.create(legalEntityIngestResponseMono).expectError().verify();
    }

    @Test
    @Tag("true")
    void ingestionInPullModeSyncProductChainDisabled_Success(TestInfo testInfo) {
        List<String> tags = new ArrayList<>(testInfo.getTags());
        Mono<LegalEntityResponse> legalEntityIngestResponseMono = executeIngestionWithPullMode(
                Boolean.valueOf(tags.get(0)), Boolean.FALSE, Boolean.FALSE);
        StepVerifier.create(legalEntityIngestResponseMono)
                .assertNext(Assertions::assertNotNull).verifyComplete();
    }

    Mono<LegalEntityResponse> executeIngestionWithPullMode(Boolean isProductChainAsync,
                                                           Boolean isProductChainEnabled, Boolean isProductChainEnabledFromRequest) {
        LegalEntityPullRequest legalEntityIngestPullRequest = LegalEntityPullRequest.builder()
                .legalEntityExternalId("externalId")
                .build();

        LegalEntityResponse res = new LegalEntityResponse(isProductChainEnabledFromRequest,
                new LegalEntity().name("legalEntityName"), null, null);
        when(legalEntityIntegrationService.pullLegalEntity(legalEntityIngestPullRequest))
                .thenReturn(Mono.just(res));

        LegalEntityTask legalEntityTask = new LegalEntityTask();
        legalEntityTask.setLegalEntity(createLE().addSubsidiariesItem(createLE()));

        when(legalEntitySaga.executeTask(any()))
                .thenReturn(Mono.just(legalEntityTask));

        if (isProductChainEnabled) {
            when(config.isProductChainAsync()).thenReturn(isProductChainAsync);
        }

        return legalEntityIngestionService
                .ingestPull(legalEntityIngestPullRequest);

    }

    private LegalEntity createLE(){
        return new LegalEntity()
            .name("legalEntityName").internalId("internalId")
            .externalId("externalId")
            .masterServiceAgreement(
                new ServiceAgreement().externalId("sa_externalId").internalId("sa_internalId"))
            .users(List.of(new JobProfileUser()
                .user(new User().internalId("user_internalId").externalId("user_externalId"))
                .referenceJobRoleNames(List.of("Admin Role"))))
            .productGroups(List.of(new ProductGroup()));
    }

    //@Test
    void ingestionInPushMode_Unsupported() {
        LegalEntityPushRequest request = LegalEntityPushRequest.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> {
            legalEntityIngestionService.ingestPush(request);
        });
    }
}
