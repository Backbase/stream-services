package com.backbase.stream.investment.service.resttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.investment.api.service.sync.ApiClient;
import com.backbase.investment.api.service.sync.v1.AssetUniverseApi;
import com.backbase.investment.api.service.sync.v1.model.AssetCategory;
import com.backbase.stream.investment.Asset;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InvestmentRestAssetUniverseServiceTest {

    @Mock
    private AssetUniverseApi assetUniverseApi;

    @Mock
    private ApiClient apiClient;

    private InvestmentRestAssetUniverseService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentRestAssetUniverseService(assetUniverseApi, apiClient);
    }

    // -----------------------------------------------------------------------
    // createAsset
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createAsset")
    class CreateAsset {

        @Test
        @DisplayName("successful create maps and returns stream asset")
        void successfulCreateReturnsMappedAsset() {
            Asset asset = new Asset();
            asset.setIsin("ISIN001");
            asset.setMarket("XNAS");
            asset.setCurrency("USD");
            asset.setName("TestAsset");

            UUID syncUuid = UUID.randomUUID();
            com.backbase.investment.api.service.sync.v1.model.Asset syncAsset =
                new com.backbase.investment.api.service.sync.v1.model.Asset(syncUuid);
            syncAsset.setName("TestAsset");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(new ResponseEntity<>(syncAsset, HttpStatus.OK));

            StepVerifier.create(service.createAsset(asset, Map.of()))
                .assertNext(result -> {
                    assertThat(result.getName()).isEqualTo("TestAsset");
                    assertThat(result.getUuid()).isEqualTo(syncUuid);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("API failure falls back to original asset and completes normally")
        void apiFailureFallsBackToOriginalAsset() {
            Asset asset = new Asset();
            asset.setIsin("ISIN001");
            asset.setMarket("XNAS");
            asset.setCurrency("USD");
            asset.setName("OriginalName");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenThrow(new RuntimeException("Network error"));

            StepVerifier.create(service.createAsset(asset, Map.of()))
                .assertNext(result -> assertThat(result).isSameAs(asset))
                .verifyComplete();
        }

        @Test
        @DisplayName("asset with logo file passes logo to form params")
        void assetWithLogoFileIncludesLogoInRequest() {
            Asset asset = new Asset();
            asset.setIsin("ISIN001");
            asset.setMarket("XNAS");
            asset.setCurrency("USD");
            asset.setName("AssetWithLogo");
            Resource logo = new ByteArrayResource("logo-bytes".getBytes()) {
                @Override
                public String getFilename() {
                    return "logo.png";
                }
            };
            asset.setLogoFile(logo);

            com.backbase.investment.api.service.sync.v1.model.Asset syncAsset =
                new com.backbase.investment.api.service.sync.v1.model.Asset(UUID.randomUUID());
            syncAsset.setName("AssetWithLogo");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(new ResponseEntity<>(syncAsset, HttpStatus.OK));

            StepVerifier.create(service.createAsset(asset, Map.of()))
                .assertNext(result -> assertThat(result.getName()).isEqualTo("AssetWithLogo"))
                .verifyComplete();
        }
    }

    // -----------------------------------------------------------------------
    // patchAsset (reactive version)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("patchAsset (reactive)")
    class PatchAsset {

        @Test
        @DisplayName("successful patch returns original stream asset")
        void successfulPatchReturnsOriginalAsset() {
            UUID existUuid = UUID.randomUUID();
            final com.backbase.investment.api.service.v1.model.Asset existAsset =
                new com.backbase.investment.api.service.v1.model.Asset(existUuid);

            Asset streamAsset = new Asset();
            streamAsset.setIsin("ISIN001");
            streamAsset.setMarket("XNAS");
            streamAsset.setCurrency("USD");
            streamAsset.setName("PatchedAsset");

            com.backbase.investment.api.service.sync.v1.model.Asset syncPatched =
                new com.backbase.investment.api.service.sync.v1.model.Asset(existUuid);
            syncPatched.setName("PatchedAsset");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenReturn(new ResponseEntity<>(syncPatched, HttpStatus.OK));

            StepVerifier.create(service.patchAsset(existAsset, streamAsset, Map.of()))
                .assertNext(result -> assertThat(result).isSameAs(streamAsset))
                .verifyComplete();
        }

        @Test
        @DisplayName("API failure on patch falls back to original asset and completes normally")
        void apiFailureOnPatchFallsBackToOriginalAsset() {
            final com.backbase.investment.api.service.v1.model.Asset existAsset =
                new com.backbase.investment.api.service.v1.model.Asset(UUID.randomUUID());

            Asset streamAsset = new Asset();
            streamAsset.setIsin("ISIN001");
            streamAsset.setMarket("XNAS");
            streamAsset.setCurrency("USD");
            streamAsset.setName("OriginalName");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenThrow(new RuntimeException("Network error"));

            StepVerifier.create(service.patchAsset(existAsset, streamAsset, Map.of()))
                .assertNext(result -> assertThat(result).isSameAs(streamAsset))
                .verifyComplete();
        }
    }

    // -----------------------------------------------------------------------
    // setAssetCategoryLogo
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("setAssetCategoryLogo")
    class SetAssetCategoryLogo {

        @Test
        @DisplayName("null logo returns Mono.empty()")
        void nullLogoReturnsEmpty() {
            UUID categoryId = UUID.randomUUID();

            StepVerifier.create(service.setAssetCategoryLogo(categoryId, null))
                .verifyComplete();

            verify(apiClient, never()).invokeAPI(anyString(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any());
        }

        @Test
        @DisplayName("logo provided sends PATCH and returns category UUID")
        void logoProvidedReturnsCategoryUuid() {
            UUID categoryId = UUID.randomUUID();
            Resource logo = new ByteArrayResource("img".getBytes()) {
                @Override
                public String getFilename() {
                    return "cat.png";
                }
            };

            AssetCategory patchedCategory = new AssetCategory(categoryId);

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(new ResponseEntity<>(patchedCategory, HttpStatus.OK));

            StepVerifier.create(service.setAssetCategoryLogo(categoryId, logo))
                .assertNext(result -> assertThat(result).isEqualTo(categoryId))
                .verifyComplete();
        }

        @Test
        @DisplayName("API failure falls back to original category ID and completes normally")
        void apiFailureFallsBackToCategoryId() {
            UUID categoryId = UUID.randomUUID();
            Resource logo = new ByteArrayResource("img".getBytes()) {
                @Override
                public String getFilename() {
                    return "cat.png";
                }
            };

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenThrow(new RuntimeException("Network error"));

            StepVerifier.create(service.setAssetCategoryLogo(categoryId, logo))
                .assertNext(result -> assertThat(result).isEqualTo(categoryId))
                .verifyComplete();
        }
    }

    // -----------------------------------------------------------------------
    // createAssetCategory
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createAssetCategory")
    class CreateAssetCategory {

        @Test
        @DisplayName("successful create returns AssetCategory")
        void successfulCreateReturnsCategory() {
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setName("Tech");
            entry.setCode("TECH");

            UUID createdUuid = UUID.randomUUID();
            AssetCategory created = new AssetCategory(createdUuid);
            created.setName("Tech");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(new ResponseEntity<>(created, HttpStatus.OK));

            StepVerifier.create(service.createAssetCategory(entry, null))
                .assertNext(result -> {
                    assertThat(result.getName()).isEqualTo("Tech");
                    assertThat(result.getUuid()).isEqualTo(createdUuid);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("category with image passes image in form params")
        void categoryWithImageIncludesImageInRequest() {
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setName("Bonds");
            entry.setCode("BONDS");

            final Resource image = new ByteArrayResource("img-data".getBytes()) {
                @Override
                public String getFilename() {
                    return "bonds.png";
                }
            };

            AssetCategory created = new AssetCategory(UUID.randomUUID());
            created.setName("Bonds");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(new ResponseEntity<>(created, HttpStatus.OK));

            StepVerifier.create(service.createAssetCategory(entry, image))
                .assertNext(result -> assertThat(result.getName()).isEqualTo("Bonds"))
                .verifyComplete();
        }
    }

    // -----------------------------------------------------------------------
    // patchAssetCategory
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("patchAssetCategory")
    class PatchAssetCategoryTest {

        @Test
        @DisplayName("successful patch returns updated AssetCategory")
        void successfulPatchReturnsUpdatedCategory() {
            UUID categoryId = UUID.randomUUID();
            AssetCategoryEntry entry = new AssetCategoryEntry();
            entry.setName("FixedIncome");
            entry.setCode("FIXED");

            AssetCategory patched = new AssetCategory(categoryId);
            patched.setName("FixedIncome");

            when(apiClient.invokeAPI(anyString(), any(), any(), any(), isNull(), any(), any(), any(), any(), any(),
                any(), any())).thenReturn(new ResponseEntity<>(patched, HttpStatus.OK));

            StepVerifier.create(service.patchAssetCategory(categoryId, entry, null))
                .assertNext(result -> {
                    assertThat(result.getUuid()).isEqualTo(categoryId);
                    assertThat(result.getName()).isEqualTo("FixedIncome");
                })
                .verifyComplete();
        }
    }

    // -----------------------------------------------------------------------
    // getFileNameForLog (static utility)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getFileNameForLog")
    class GetFileNameForLog {

        @Test
        @DisplayName("null resource returns 'null' string")
        void nullResourceReturnsNullString() {
            assertThat(InvestmentRestAssetUniverseService.getFileNameForLog(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("resource with filename returns filename")
        void resourceWithFilenameReturnsFilename() {
            Resource logo = new ByteArrayResource("data".getBytes()) {
                @Override
                public String getFilename() {
                    return "my-logo.png";
                }
            };
            assertThat(InvestmentRestAssetUniverseService.getFileNameForLog(logo)).isEqualTo("my-logo.png");
        }
    }
}

