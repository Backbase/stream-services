package com.backbase.stream.investment.service;

import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
//@ActiveProfiles({"it", "moustache-bank", "moustache-bank-subsidiaries"})
class InvestmentSagaTest {
/*
//    @Autowired
    InvestmentClientService clientService;
    InvestmentSagaConfigurationProperties properties;
    InvestmentSaga saga;

    @BeforeEach
    void setUp() {
//        clientService = Mockito.mock(InvestmentClientService.class);
        properties = new InvestmentSagaConfigurationProperties();
        saga = new InvestmentSaga(clientService, properties);
    }

    @Test
    @Disabled("Spec currently does not include UUID field")
    void createClient_success_noExistenceCheck() {
        ClientCreateRequest request = new ClientCreateRequest();
        ClientCreate created = new ClientCreate(UUID.randomUUID());
//        when(clientService.createClient(any())).thenReturn(Mono.just(created));

        InvestmentClientTask task = saga.newCreateTask(request);
        saga.process(task);

//        StepVerifier.create()
//            .assertNext(t -> {
//                assertThat(t.getState()).isEqualTo(InvestmentClientTask.State.COMPLETED);
//                assertThat(t.getCreatedClient()).isNotNull();
//            })
//            .verifyComplete();
    }

    @Test
    @Disabled("Spec currently does not include UUID field")
    void createClient_skipped_whenExists() throws Exception {
        properties.setPreExistenceCheck(true);
        ClientCreateRequest request = new ClientCreateRequest();
        UUID uuid = UUID.randomUUID();
        setUuidReflectively(request, uuid);
        when(clientService.getClient(eq(uuid))).thenReturn(Mono.just(new OASClient()));

        InvestmentClientTask task = saga.newCreateTask(request);

        StepVerifier.create(saga.process(task))
            .assertNext(t -> {
                assertThat(t.getState()).isEqualTo(InvestmentClientTask.State.COMPLETED);
                assertThat(t.getCreatedClient()).isNull();
                assertThat(t.getHistory()).anyMatch(h -> "skipped".equals(h.getResult()));
            })
            .verifyComplete();
    }

    @Test
    @Disabled("Spec currently does not include UUID field")
    void createClient_failure() {
        ClientCreateRequest request = new ClientCreateRequest();
        when(clientService.createClient(any())).thenReturn(Mono.error(new RuntimeException("boom")));

        InvestmentClientTask task = saga.newCreateTask(request);

        StepVerifier.create(saga.process(task))
            .assertNext(t -> assertThat(t.getState()).isEqualTo(InvestmentClientTask.State.FAILED))
            .verifyComplete();
    }

    @Test
    @Disabled("Spec currently does not include UUID field")
    void patchClient_success() {
        UUID uuid = UUID.randomUUID();
        PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest();
        when(clientService.patchClient(eq(uuid), any())).thenReturn(Mono.just(new OASClient()));

        InvestmentClientTask task = saga.newPatchTask(uuid, patch);

        StepVerifier.create(saga.process(task))
            .assertNext(t -> {
                assertThat(t.getState()).isEqualTo(InvestmentClientTask.State.COMPLETED);
                assertThat(t.getUpdatedClient()).isNotNull();
            })
            .verifyComplete();
    }

    @Test
    @Disabled("Spec currently does not include UUID field")
    void patchClient_notFoundFails() {
        UUID uuid = UUID.randomUUID();
        PatchedOASClientUpdateRequest patch = new PatchedOASClientUpdateRequest();
        WebClientResponseException notFound = new WebClientResponseException(404, "Not Found", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        when(clientService.patchClient(eq(uuid), any())).thenReturn(Mono.error(notFound));

        InvestmentClientTask task = saga.newPatchTask(uuid, patch);

        StepVerifier.create(saga.process(task))
            .assertNext(t -> assertThat(t.getState()).isEqualTo(InvestmentClientTask.State.FAILED))
            .verifyComplete();
    }

    private void setUuidReflectively(Object target, UUID uuid) throws Exception {
        try {
            Field f = target.getClass().getDeclaredField("uuid");
            f.setAccessible(true);
            f.set(target, uuid);
        } catch (NoSuchFieldException ignored) {
            // If spec changes and field not present, test should fail explicitly
            throw ignored;
        }
    }*/
}

