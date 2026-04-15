package com.backbase.stream.investment;

import static org.assertj.core.api.Assertions.assertThat;

import com.backbase.investment.api.service.v1.model.AssetCategoryType;
import com.backbase.investment.api.service.v1.model.Currency;
import com.backbase.investment.api.service.v1.model.GroupResult;
import com.backbase.investment.api.service.v1.model.Market;
import com.backbase.investment.api.service.v1.model.MarketSpecialDay;
import com.backbase.stream.investment.model.AssetCategoryEntry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InvestmentAssetDataTest {

    @Nested
    class GetAssetByUuid {

        @Test
        void shouldReturnEmptyMapWhenAssetsIsNull() {
            var data = InvestmentAssetData.builder().build();

            Map<UUID, Asset> result = data.getAssetByUuid();

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyMapWhenAssetsIsEmpty() {
            var data = InvestmentAssetData.builder()
                .assets(List.of())
                .build();

            assertThat(data.getAssetByUuid()).isEmpty();
        }

        @Test
        void shouldReturnMapKeyedByUuid() {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            Asset asset1 = Asset.builder().uuid(uuid1).isin("ISIN1").build();
            Asset asset2 = Asset.builder().uuid(uuid2).isin("ISIN2").build();

            var data = InvestmentAssetData.builder()
                .assets(List.of(asset1, asset2))
                .build();

            Map<UUID, Asset> result = data.getAssetByUuid();

            assertThat(result).hasSize(2)
                .containsEntry(uuid1, asset1)
                .containsEntry(uuid2, asset2);
        }
    }

    @Nested
    class GetTotalProcessedValues {

        @Test
        void shouldReturnZeroWhenAllListsAreNull() {
            var data = InvestmentAssetData.builder().build();

            assertThat(data.getTotalProcessedValues()).isZero();
        }

        @Test
        void shouldReturnZeroWhenAllListsAreEmpty() {
            var data = InvestmentAssetData.builder()
                .currencies(List.of())
                .markets(List.of())
                .marketSpecialDays(List.of())
                .assetCategoryTypes(List.of())
                .assetCategories(List.of())
                .assets(List.of())
                .build();

            assertThat(data.getTotalProcessedValues()).isZero();
        }

        @Test
        void shouldSumSizesOfAllPopulatedLists() {
            var data = InvestmentAssetData.builder()
                .currencies(List.of(new Currency(), new Currency()))
                .markets(List.of(new Market()))
                .marketSpecialDays(List.of(new MarketSpecialDay(), new MarketSpecialDay(), new MarketSpecialDay()))
                .assetCategoryTypes(List.of(new AssetCategoryType()))
                .assetCategories(List.of(new AssetCategoryEntry()))
                .assets(List.of(Asset.builder().uuid(UUID.randomUUID()).build(),
                    Asset.builder().uuid(UUID.randomUUID()).build()))
                .build();

            assertThat(data.getTotalProcessedValues()).isEqualTo(10L);
        }

        @Test
        void shouldIgnoreNullListsAndCountOnlyPresent() {
            var data = InvestmentAssetData.builder()
                .currencies(List.of(new Currency()))
                .assets(List.of(Asset.builder().uuid(UUID.randomUUID()).build(),
                    Asset.builder().uuid(UUID.randomUUID()).build()))
                .build();

            assertThat(data.getTotalProcessedValues()).isEqualTo(3L);
        }
    }

    @Nested
    class BuilderAndEquality {

        @Test
        void shouldBuildWithAllFields() {
            UUID uuid = UUID.randomUUID();
            Asset asset = Asset.builder().uuid(uuid).build();
            GroupResult groupResult = new GroupResult();

            var data = InvestmentAssetData.builder()
                .currencies(List.of(new Currency()))
                .markets(List.of(new Market()))
                .marketSpecialDays(List.of(new MarketSpecialDay()))
                .assetCategoryTypes(List.of(new AssetCategoryType()))
                .assetCategories(List.of(new AssetCategoryEntry()))
                .assets(List.of(asset))
                .priceAsyncTasks(List.of(groupResult))
                .intradayPriceAsyncTasks(List.of(groupResult))
                .build();

            assertThat(data.getCurrencies()).hasSize(1);
            assertThat(data.getMarkets()).hasSize(1);
            assertThat(data.getMarketSpecialDays()).hasSize(1);
            assertThat(data.getAssetCategoryTypes()).hasSize(1);
            assertThat(data.getAssetCategories()).hasSize(1);
            assertThat(data.getAssets()).containsExactly(asset);
            assertThat(data.getPriceAsyncTasks()).containsExactly(groupResult);
            assertThat(data.getIntradayPriceAsyncTasks()).containsExactly(groupResult);
        }

        @Test
        void shouldBeEqualWhenFieldsMatch() {
            var data1 = InvestmentAssetData.builder()
                .currencies(List.of(new Currency()))
                .build();
            var data2 = InvestmentAssetData.builder()
                .currencies(List.of(new Currency()))
                .build();

            assertThat(data1).isEqualTo(data2);
            assertThat(data1).hasSameHashCodeAs(data2);
        }

        @Test
        void shouldNotBeEqualWhenFieldsDiffer() {
            var data1 = InvestmentAssetData.builder()
                .currencies(List.of(new Currency()))
                .build();
            var data2 = InvestmentAssetData.builder()
                .currencies(List.of(new Currency(), new Currency()))
                .build();

            assertThat(data1).isNotEqualTo(data2);
        }

        @Test
        void shouldSupportSetters() {
            var data = new InvestmentAssetData.InvestmentAssetDataBuilder().build();
            List<Currency> currencies = List.of(new Currency());

            data.setCurrencies(currencies);
            data.setMarkets(List.of(new Market()));

            assertThat(data.getCurrencies()).isEqualTo(currencies);
            assertThat(data.getMarkets()).hasSize(1);
        }

        @Test
        void shouldSupportAllSetters() {
            var data = InvestmentAssetData.builder().build();
            List<MarketSpecialDay> specialDays = List.of(new MarketSpecialDay());
            List<AssetCategoryType> categoryTypes = List.of(new AssetCategoryType());
            List<AssetCategoryEntry> categories = List.of(new AssetCategoryEntry());
            List<Asset> assets = List.of(Asset.builder().uuid(UUID.randomUUID()).build());
            List<GroupResult> priceTasks = List.of(new GroupResult());
            List<GroupResult> intradayTasks = List.of(new GroupResult());

            data.setMarketSpecialDays(specialDays);
            data.setAssetCategoryTypes(categoryTypes);
            data.setAssetCategories(categories);
            data.setAssets(assets);
            data.setPriceAsyncTasks(priceTasks);
            data.setIntradayPriceAsyncTasks(intradayTasks);

            assertThat(data.getMarketSpecialDays()).isEqualTo(specialDays);
            assertThat(data.getAssetCategoryTypes()).isEqualTo(categoryTypes);
            assertThat(data.getAssetCategories()).isEqualTo(categories);
            assertThat(data.getAssets()).isEqualTo(assets);
            assertThat(data.getPriceAsyncTasks()).isEqualTo(priceTasks);
            assertThat(data.getIntradayPriceAsyncTasks()).isEqualTo(intradayTasks);
        }

        @Test
        void shouldNotBeEqualToNull() {
            var data = InvestmentAssetData.builder().currencies(List.of(new Currency())).build();

            assertThat(data).isNotEqualTo(null);
        }

        @Test
        void shouldNotBeEqualToDifferentType() {
            var data = InvestmentAssetData.builder().currencies(List.of(new Currency())).build();

            assertThat(data).isNotEqualTo("not an InvestmentAssetData");
        }

        @Test
        void shouldBeEqualToItself() {
            var data = InvestmentAssetData.builder().currencies(List.of(new Currency())).build();

            assertThat(data).isEqualTo(data);
        }

        @Test
        void shouldProduceToString() {
            var data = InvestmentAssetData.builder()
                .currencies(List.of(new Currency()))
                .build();

            assertThat(data.toString()).contains("InvestmentAssetData");
        }
    }

    @Nested
    class AsyncTasksExcludedFromTotal {

        @Test
        void shouldNotCountPriceAsyncTasksInTotal() {
            var data = InvestmentAssetData.builder()
                .assets(List.of(Asset.builder().uuid(UUID.randomUUID()).build()))
                .priceAsyncTasks(List.of(new GroupResult(), new GroupResult()))
                .intradayPriceAsyncTasks(List.of(new GroupResult()))
                .build();

            assertThat(data.getTotalProcessedValues()).isEqualTo(1L);
        }
    }
}
