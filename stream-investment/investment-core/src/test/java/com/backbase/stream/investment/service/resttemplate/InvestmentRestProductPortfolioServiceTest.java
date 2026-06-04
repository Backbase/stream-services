package com.backbase.stream.investment.service.resttemplate;

import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_ADVICE_ENGINE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_DESCRIPTION;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_EXTERNAL_ID;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_EXTRA_DATA;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_IMAGE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_MODEL_PORTFOLIO;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_NAME;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_ORDER;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_PRODUCT_CATEGORY;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_PRODUCT_TYPE;
import static com.backbase.investment.api.service.sync.v1.model.PortfolioProduct.JSON_PROPERTY_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.v1.model.InvestorModelPortfolio;
import com.backbase.investment.api.service.v1.model.PortfolioProduct;
import com.backbase.investment.api.service.v1.model.PortfolioProductStatusEnum;
import com.backbase.investment.api.service.v1.model.ProductTypeEnum;
import com.backbase.stream.configuration.IngestConfigProperties;
import com.backbase.stream.investment.ProductPortfolio;
import java.util.List;
import java.util.Map;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvestmentRestProductPortfolioServiceTest {

    private static final String CREATE_PATH = "/service-api/v2/products/portfolio/";
    private static final String UPDATE_PATH = "/service-api/v2/products/portfolio/{uuid}/";
    private static final List<String> EXPAND = List.of("model_portfolio.allocation.asset");

    @Mock
    private ApiClient apiClient;

    @Captor
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<MultiValueMap<String, Object>> formParamsCaptor;

    @Captor
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<MultiValueMap<String, String>> queryParamsCaptor;

    private final IngestConfigProperties ingestProperties = new IngestConfigProperties();

    private InvestmentRestProductPortfolioService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentRestProductPortfolioService(apiClient, ingestProperties);
        when(apiClient.selectHeaderAccept(any())).thenReturn(List.of(MediaType.APPLICATION_JSON));
        when(apiClient.selectHeaderContentType(any())).thenReturn(MediaType.MULTIPART_FORM_DATA);
        when(apiClient.parameterToMultiValueMap(any(), eq("expand"), any()))
            .thenReturn(new LinkedMultiValueMap<>());
    }

    @Nested
    @DisplayName("createPortfolioProduct")
    class CreatePortfolioProduct {

        @Test
        @DisplayName("successful create returns the created PortfolioProduct")
        void successfulCreateReturnsResponse() {
            UUID createdUuid = UUID.randomUUID();
            ProductPortfolio template = buildFullTemplate(createdUuid);
            PortfolioProduct response = buildApiProduct(createdUuid, "Robo Plan", ProductTypeEnum.ROBO_ADVISOR);

            when(apiClient.invokeAPI(eq(CREATE_PATH), eq(HttpMethod.POST), any(), queryParamsCaptor.capture(),
                any(), any(), any(), formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

            StepVerifier.create(service.createPortfolioProduct(template, EXPAND))
                .assertNext(result -> {
                    assertThat(result.getUuid()).isEqualTo(createdUuid);
                    assertThat(result.getName()).isEqualTo("Robo Plan");
                    assertThat(result.getProductType()).isEqualTo(ProductTypeEnum.ROBO_ADVISOR);
                })
                .verifyComplete();

            assertFormParamsMatchTemplate(formParamsCaptor.getValue(), createdUuid);
            assertThat(queryParamsCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("create uses direct JSON_PROPERTY form param names")
        void createUsesDirectFormParamNames() {
            ProductPortfolio template = buildFullTemplate(UUID.randomUUID());

            when(apiClient.invokeAPI(eq(CREATE_PATH), eq(HttpMethod.POST), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(
                    buildApiProduct(UUID.randomUUID(), "Robo Plan", ProductTypeEnum.ROBO_ADVISOR), HttpStatus.OK));

            StepVerifier.create(service.createPortfolioProduct(template, EXPAND)).expectNextCount(1).verifyComplete();

            MultiValueMap<String, Object> formParams = formParamsCaptor.getValue();
            assertThat(formParams.keySet()).contains(
                JSON_PROPERTY_NAME,
                JSON_PROPERTY_DESCRIPTION,
                JSON_PROPERTY_EXTERNAL_ID,
                JSON_PROPERTY_STATUS,
                JSON_PROPERTY_ORDER,
                JSON_PROPERTY_ADVICE_ENGINE,
                JSON_PROPERTY_MODEL_PORTFOLIO,
                JSON_PROPERTY_PRODUCT_TYPE,
                JSON_PROPERTY_PRODUCT_CATEGORY,
                JSON_PROPERTY_EXTRA_DATA);
            assertThat(formParams.getFirst(JSON_PROPERTY_NAME)).isEqualTo("Robo Plan");
            assertThat(formParams.getFirst(JSON_PROPERTY_PRODUCT_TYPE)).isEqualTo("robo-advisor");
            assertThat(formParams.getFirst(JSON_PROPERTY_PRODUCT_CATEGORY)).isEqualTo("retail");
            assertThat(formParams.getFirst(JSON_PROPERTY_MODEL_PORTFOLIO))
                .isEqualTo(template.getModelPortfolio().getUuid().toString());
        }

        @Test
        @DisplayName("create includes image form param when ingestImages is enabled")
        void createIncludesImageWhenIngestEnabled() {
            ingestProperties.getPortfolio().setIngestImages(true);
            ProductPortfolio template = buildFullTemplate(UUID.randomUUID());
            template.setImageResource(new ByteArrayResource("logo".getBytes()) {
                @Override
                public String getFilename() {
                    return "logo.png";
                }
            });

            when(apiClient.invokeAPI(any(), eq(HttpMethod.POST), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(
                    buildApiProduct(UUID.randomUUID(), "Robo Plan", ProductTypeEnum.ROBO_ADVISOR), HttpStatus.OK));

            StepVerifier.create(service.createPortfolioProduct(template, EXPAND)).expectNextCount(1).verifyComplete();

            assertThat(formParamsCaptor.getValue().containsKey(JSON_PROPERTY_IMAGE)).isTrue();
        }

        @Test
        @DisplayName("create omits image form param when ingestImages is disabled")
        void createOmitsImageWhenIngestDisabled() {
            ingestProperties.getPortfolio().setIngestImages(false);
            ProductPortfolio template = buildFullTemplate(UUID.randomUUID());
            template.setImageResource(new ByteArrayResource("logo".getBytes()));

            when(apiClient.invokeAPI(any(), eq(HttpMethod.POST), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(
                    buildApiProduct(UUID.randomUUID(), "Robo Plan", ProductTypeEnum.ROBO_ADVISOR), HttpStatus.OK));

            StepVerifier.create(service.createPortfolioProduct(template, EXPAND)).expectNextCount(1).verifyComplete();

            assertThat(formParamsCaptor.getValue().containsKey(JSON_PROPERTY_IMAGE)).isFalse();
        }

        @Test
        @DisplayName("API failure propagates as error signal")
        void apiFailurePropagatesError() {
            ProductPortfolio template = buildFullTemplate(UUID.randomUUID());

            when(apiClient.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Network error"));

            StepVerifier.create(service.createPortfolioProduct(template, EXPAND))
                .expectErrorMessage("Network error")
                .verify();
        }

        @Test
        @DisplayName("API returns null body propagates NullPointerException")
        void nullResponseBodyPropagatesNullPointer() {
            ProductPortfolio template = buildFullTemplate(UUID.randomUUID());

            when(apiClient.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

            StepVerifier.create(service.createPortfolioProduct(template, EXPAND))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("updatePortfolioProduct")
    class UpdatePortfolioProduct {

        @Test
        @DisplayName("successful update returns the patched PortfolioProduct")
        void successfulUpdateReturnsResponse() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildFullTemplate(existingUuid);
            PortfolioProduct response = buildApiProduct(existingUuid, "Robo Plan", ProductTypeEnum.ROBO_ADVISOR);

            when(apiClient.invokeAPI(eq(UPDATE_PATH), eq(HttpMethod.PUT), any(), queryParamsCaptor.capture(),
                any(), any(), any(), formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

            StepVerifier.create(service.updatePortfolioProduct(existingUuid.toString(), EXPAND, template))
                .assertNext(result -> {
                    assertThat(result.getUuid()).isEqualTo(existingUuid);
                    assertThat(result.getName()).isEqualTo("Robo Plan");
                })
                .verifyComplete();

            assertFormParamsMatchTemplate(formParamsCaptor.getValue(), existingUuid);
            verify(apiClient).invokeAPI(
                eq(UPDATE_PATH), eq(HttpMethod.PUT), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("update uses direct JSON_PROPERTY form param names")
        void updateUsesDirectFormParamNames() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildFullTemplate(existingUuid);

            when(apiClient.invokeAPI(eq(UPDATE_PATH), eq(HttpMethod.PUT), any(), any(), any(), any(), any(),
                formParamsCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(
                    buildApiProduct(existingUuid, "Robo Plan", ProductTypeEnum.ROBO_ADVISOR), HttpStatus.OK));

            StepVerifier.create(service.updatePortfolioProduct(existingUuid.toString(), EXPAND, template))
                .expectNextCount(1)
                .verifyComplete();

            assertThat(formParamsCaptor.getValue().getFirst(JSON_PROPERTY_NAME)).isEqualTo("Robo Plan");
            assertThat(formParamsCaptor.getValue().getFirst(JSON_PROPERTY_PRODUCT_TYPE)).isEqualTo("robo-advisor");
        }

        @Test
        @DisplayName("null UUID emits HttpClientErrorException with BAD_REQUEST status")
        void nullUuidEmitsHttpClientErrorException() {
            ProductPortfolio template = buildFullTemplate(UUID.randomUUID());

            StepVerifier.create(service.updatePortfolioProduct(null, EXPAND, template))
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
            ProductPortfolio template = buildFullTemplate(existingUuid);

            when(apiClient.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

            StepVerifier.create(service.updatePortfolioProduct(existingUuid.toString(), EXPAND, template))
                .expectErrorMessage("Connection refused")
                .verify();
        }

        @Test
        @DisplayName("API returns null body propagates NullPointerException")
        void nullResponseBodyPropagatesNullPointer() {
            UUID existingUuid = UUID.randomUUID();
            ProductPortfolio template = buildFullTemplate(existingUuid);

            when(apiClient.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

            StepVerifier.create(service.updatePortfolioProduct(existingUuid.toString(), EXPAND, template))
                .expectError(NullPointerException.class)
                .verify();
        }
    }

    private ProductPortfolio buildFullTemplate(UUID modelPortfolioUuid) {
        ProductPortfolio template = new ProductPortfolio();
        template.setName("Robo Plan");
        template.setDescription("Robo description");
        template.setProductType(ProductTypeEnum.ROBO_ADVISOR);
        template.setProductCategory("retail");
        template.setOrder(1);
        template.setExternalId("ext-robo-001");
        template.setStatus(PortfolioProductStatusEnum.ACTIVE);
        template.setAdviceEngine("default-engine");
        template.setExtraData(Map.of("key", "value"));
        template.setModelPortfolio(new InvestorModelPortfolio(
            modelPortfolioUuid, "Growth Model", 0.25, 7, null, null, null));
        return template;
    }

    private void assertFormParamsMatchTemplate(MultiValueMap<String, Object> formParams, UUID modelPortfolioUuid) {
        assertThat(formParams.getFirst(JSON_PROPERTY_NAME)).isEqualTo("Robo Plan");
        assertThat(formParams.getFirst(JSON_PROPERTY_DESCRIPTION)).isEqualTo("Robo description");
        assertThat(formParams.getFirst(JSON_PROPERTY_EXTERNAL_ID)).isEqualTo("ext-robo-001");
        assertThat(formParams.getFirst(JSON_PROPERTY_STATUS)).isEqualTo(PortfolioProductStatusEnum.ACTIVE.getValue());
        assertThat(formParams.getFirst(JSON_PROPERTY_ORDER)).isEqualTo(1);
        assertThat(formParams.getFirst(JSON_PROPERTY_ADVICE_ENGINE)).isEqualTo("default-engine");
        assertThat(formParams.getFirst(JSON_PROPERTY_PRODUCT_TYPE)).isEqualTo("robo-advisor");
        assertThat(formParams.getFirst(JSON_PROPERTY_PRODUCT_CATEGORY)).isEqualTo("retail");
        assertThat(formParams.getFirst(JSON_PROPERTY_MODEL_PORTFOLIO)).isEqualTo(modelPortfolioUuid.toString());
        assertThat(formParams.getFirst(JSON_PROPERTY_EXTRA_DATA)).isEqualTo(Map.of("key", "value"));
    }

    private PortfolioProduct buildApiProduct(UUID uuid, String name, ProductTypeEnum productType) {
        return new PortfolioProduct(
            name, null, null, 1, null, "retail", uuid, null, null, productType);
    }
}
