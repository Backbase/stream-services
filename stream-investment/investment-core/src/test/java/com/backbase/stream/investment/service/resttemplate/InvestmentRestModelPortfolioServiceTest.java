package com.backbase.stream.investment.service.resttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioRequestDataRequest;
import com.backbase.investment.api.service.v1.model.OASModelPortfolioResponse;
import com.backbase.stream.investment.ModelPortfolio;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InvestmentRestModelPortfolioServiceTest {

    private static final String FORM_PARAM_DATA = "data";

    @Mock
    private ApiClient apiClient;

    @Captor
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<MultiValueMap<String, Object>> formParamsCaptor;

    private InvestmentRestModelPortfolioService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentRestModelPortfolioService(apiClient);
    }

    // -----------------------------------------------------------------------
    // createModelPortfolio
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createModelPortfolio")
    class CreateModelPortfolio {

        @Test
        @DisplayName("successful create returns the created OASModelPortfolioResponse")
        void successfulCreateReturnsResponse() {
            ModelPortfolio modelPortfolio = buildModelPortfolio("Conservative", 3);

            UUID createdUuid = UUID.randomUUID();
            OASModelPortfolioResponse response = new OASModelPortfolioResponse(createdUuid);
            response.setName("Conservative");
            response.setRiskLevel(3);

            when(apiClient.invokeAPI(anyString(), eq(HttpMethod.POST), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

            StepVerifier.create(service.createModelPortfolio(modelPortfolio))
                .assertNext(result -> {
                    assertThat(result.getUuid()).isEqualTo(createdUuid);
                    assertThat(result.getName()).isEqualTo("Conservative");
                    assertThat(result.getRiskLevel()).isEqualTo(3);
                })
                .verifyComplete();

            assertThat(formParamsCaptor.getValue().containsKey(FORM_PARAM_DATA)).isTrue();
            assertThat(formParamsCaptor.getValue().getFirst(FORM_PARAM_DATA))
                .isInstanceOf(OASModelPortfolioRequestDataRequest.class);
            OASModelPortfolioRequestDataRequest dataRequest =
                (OASModelPortfolioRequestDataRequest) formParamsCaptor.getValue().getFirst(FORM_PARAM_DATA);
            assertThat(dataRequest.getName()).isEqualTo("Conservative");
            assertThat(dataRequest.getRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("create sends multipart form field named 'data'")
        void createUsesDataFormParamName() {
            ModelPortfolio modelPortfolio = buildModelPortfolio("Growth", 7);

            when(apiClient.invokeAPI(anyString(), eq(HttpMethod.POST), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(
                    new OASModelPortfolioResponse(UUID.randomUUID()), HttpStatus.OK));

            StepVerifier.create(service.createModelPortfolio(modelPortfolio)).expectNextCount(1).verifyComplete();

            assertThat(formParamsCaptor.getValue().keySet()).containsExactly(FORM_PARAM_DATA);
        }

        @Test
        @DisplayName("API failure propagates as error signal")
        void apiFailurePropagatesError() {
            ModelPortfolio modelPortfolio = buildModelPortfolio("Growth", 7);

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenThrow(new RuntimeException("Network error"));

            StepVerifier.create(service.createModelPortfolio(modelPortfolio))
                .expectErrorMessage("Network error")
                .verify();
        }

        @Test
        @DisplayName("API returns null body propagates NullPointerException")
        void nullResponseBodyPropagatesNullPointer() {
            ModelPortfolio modelPortfolio = buildModelPortfolio("Balanced", 5);

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));

            StepVerifier.create(service.createModelPortfolio(modelPortfolio))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    // -----------------------------------------------------------------------
    // patchModelPortfolio
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("patchModelPortfolio")
    class PatchModelPortfolio {

        @Test
        @DisplayName("successful patch returns the patched OASModelPortfolioResponse")
        void successfulPatchReturnsResponse() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio modelPortfolio = buildModelPortfolio("Dynamic", 8);

            OASModelPortfolioResponse response = new OASModelPortfolioResponse(existingUuid);
            response.setName("Dynamic");
            response.setRiskLevel(8);

            when(apiClient.invokeAPI(anyString(), eq(HttpMethod.PUT), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

            StepVerifier.create(service.patchModelPortfolio(existingUuid.toString(), modelPortfolio))
                .assertNext(result -> {
                    assertThat(result.getUuid()).isEqualTo(existingUuid);
                    assertThat(result.getName()).isEqualTo("Dynamic");
                    assertThat(result.getRiskLevel()).isEqualTo(8);
                })
                .verifyComplete();

            assertThat(formParamsCaptor.getValue().containsKey(FORM_PARAM_DATA)).isTrue();
            assertThat(formParamsCaptor.getValue().getFirst(FORM_PARAM_DATA))
                .isInstanceOf(OASModelPortfolioRequestDataRequest.class);
            OASModelPortfolioRequestDataRequest dataRequest =
                (OASModelPortfolioRequestDataRequest) formParamsCaptor.getValue().getFirst(FORM_PARAM_DATA);
            assertThat(dataRequest.getName()).isEqualTo("Dynamic");
            assertThat(dataRequest.getRiskLevel()).isEqualTo(8);
        }

        @Test
        @DisplayName("patch sends multipart form field named 'data'")
        void patchUsesDataFormParamName() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio modelPortfolio = buildModelPortfolio("Income", 2);

            when(apiClient.invokeAPI(anyString(), eq(HttpMethod.PUT), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(
                    new OASModelPortfolioResponse(existingUuid), HttpStatus.OK));

            StepVerifier.create(service.patchModelPortfolio(existingUuid.toString(), modelPortfolio))
                .expectNextCount(1)
                .verifyComplete();

            assertThat(formParamsCaptor.getValue().keySet()).containsExactly(FORM_PARAM_DATA);
            verify(apiClient).invokeAPI(
                eq("/service-api/v2/advice-engines/model-portfolio/model_portfolios/{uuid}/"),
                eq(HttpMethod.PUT), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("null UUID emits HttpClientErrorException with BAD_REQUEST status")
        void nullUuidEmitsHttpClientErrorException() {
            ModelPortfolio modelPortfolio = buildModelPortfolio("Stable", 4);

            StepVerifier.create(service.patchModelPortfolio(null, modelPortfolio))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(HttpClientErrorException.class);
                    HttpClientErrorException ex = (HttpClientErrorException) err;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .verify();
        }

        @Test
        @DisplayName("API failure propagates as error signal")
        void apiFailurePropagatesError() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio modelPortfolio = buildModelPortfolio("Income", 2);

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenThrow(new RuntimeException("Connection refused"));

            StepVerifier.create(service.patchModelPortfolio(existingUuid.toString(), modelPortfolio))
                .expectErrorMessage("Connection refused")
                .verify();
        }

        @Test
        @DisplayName("API returns null body propagates NullPointerException")
        void nullResponseBodyPropagatesNullPointer() {
            UUID existingUuid = UUID.randomUUID();
            ModelPortfolio modelPortfolio = buildModelPortfolio("Aggressive", 9);

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));

            StepVerifier.create(service.patchModelPortfolio(existingUuid.toString(), modelPortfolio))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private ModelPortfolio buildModelPortfolio(String name, int riskLevel) {
        return ModelPortfolio.builder()
            .name(name)
            .riskLevel(riskLevel)
            .cashWeight(0.2)
            .build();
    }
}

