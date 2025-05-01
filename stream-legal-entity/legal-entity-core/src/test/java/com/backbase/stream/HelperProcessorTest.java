package com.backbase.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.customerprofile.api.integration.v1.model.PartyResponseUpsertDto;
import com.backbase.stream.legalentity.model.Party;
import com.backbase.stream.legalentity.model.Party.PartyTypeEnum;
import com.backbase.stream.service.CustomerProfileService;
import com.backbase.stream.worker.model.StreamTask;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HelperProcessorTest {

    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private StreamTask streamTask;

    @InjectMocks
    private HelperProcessor helperProcessor;

    private Party party1;
    private Party party2;
    private final String internalId = "int-123";
    private final String externalId = "ext-abc";

    @BeforeEach
    void setUp() {
        party1 = new Party("party-001", true, PartyTypeEnum.PERSON);
        party2 = new Party("party-002", true, PartyTypeEnum.PERSON);

    }

    @Nested
    @DisplayName("When parties list is empty or null")
    class EmptyOrNullParties {

        @Test
        @DisplayName("should log skipped and return immediately for empty list")
        void processParties_emptyList_shouldSkip() {
            List<Party> emptyList = Collections.emptyList();

            Mono<StreamTask> result = helperProcessor.processParties(
                streamTask, emptyList, internalId, externalId, customerProfileService
            );

            StepVerifier.create(result)
                .expectNext(streamTask)
                .verifyComplete();

            verify(customerProfileService, never()).upsertParty(any(), anyString());
        }

        @Test
        @DisplayName("should log skipped and return immediately for null list")
        void processParties_nullList_shouldSkip() {
            Mono<StreamTask> result = helperProcessor.processParties(
                streamTask, null, internalId, externalId, customerProfileService
            );

            StepVerifier.create(result)
                .expectNext(streamTask)
                .verifyComplete();

            verify(customerProfileService, never()).upsertParty(any(), anyString());
        }
    }

    @Nested
    @DisplayName("When processing valid parties")
    class ValidParties {

        @Test
        @DisplayName("should process all parties successfully")
        void processParties_allSuccess() {
            List<Party> parties = List.of(party1, party2);
            when(customerProfileService.upsertParty(any(Party.class), eq(internalId)))
                .thenReturn(Mono.just(new PartyResponseUpsertDto()));

            Mono<StreamTask> result = helperProcessor.processParties(
                streamTask, parties, internalId, externalId, customerProfileService
            );

            StepVerifier.create(result)
                .expectNext(streamTask)
                .verifyComplete();

            verify(customerProfileService, times(2)).upsertParty(any(Party.class), eq(internalId));

            verify(streamTask, never()).warn(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), any());
            verify(streamTask, never()).error(anyString(), anyString(), anyString(), anyString(), any(),
                any(Throwable.class), anyString(), any());
        }

        @Test
        @DisplayName("should handle one error and complete with warning")
        void processParties_oneError() {
            List<Party> parties = List.of(party1, party2);
            RuntimeException dbError = new RuntimeException("DB connection failed");

            when(customerProfileService.upsertParty(eq(party1), eq(internalId)))
                .thenReturn(Mono.just(new PartyResponseUpsertDto()));
            when(customerProfileService.upsertParty(eq(party2), eq(internalId)))
                .thenReturn(Mono.error(dbError));

            Mono<StreamTask> result = helperProcessor.processParties(
                streamTask, parties, internalId, externalId, customerProfileService
            );

            StepVerifier.create(result)
                .expectNext(streamTask)
                .verifyComplete();

            verify(customerProfileService, times(2)).upsertParty(any(Party.class), eq(internalId));

            verify(streamTask, never()).info(anyString(), anyString(), eq("completed_successfully"), anyString(),
                anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should handle all errors and complete with warning")
        void processParties_allErrors() {
            List<Party> parties = List.of(party1, party2);
            RuntimeException error1 = new RuntimeException("Error 1");
            RuntimeException error2 = new RuntimeException("Error 2");

            when(customerProfileService.upsertParty(eq(party1), eq(internalId)))
                .thenReturn(Mono.error(error1));
            when(customerProfileService.upsertParty(eq(party2), eq(internalId)))
                .thenReturn(Mono.error(error2));

            Mono<StreamTask> result = helperProcessor.processParties(
                streamTask, parties, internalId, externalId, customerProfileService
            );

            StepVerifier.create(result)
                .expectNext(streamTask)
                .verifyComplete();

            verify(customerProfileService, times(2)).upsertParty(any(Party.class), eq(internalId));

            verify(streamTask, never()).info(anyString(), anyString(), eq("upserted"), anyString(), any(), anyString(),
                any());
            verify(streamTask, never()).info(anyString(), anyString(), eq("completed_successfully"), anyString(),
                anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should filter out null parties from the list")
        void processParties_withNullInList() {
            List<Party> parties = List.of(party1, party2);
            when(customerProfileService.upsertParty(any(Party.class), eq(internalId)))
                .thenReturn(Mono.just(new PartyResponseUpsertDto()));

            Mono<StreamTask> result = helperProcessor.processParties(
                streamTask, parties, internalId, externalId, customerProfileService
            );

            StepVerifier.create(result)
                .expectNext(streamTask)
                .verifyComplete();

            verify(customerProfileService, times(2)).upsertParty(any(Party.class), eq(internalId));
            verify(customerProfileService).upsertParty(eq(party1), eq(internalId));
            verify(customerProfileService).upsertParty(eq(party2), eq(internalId));
        }
    }
}
