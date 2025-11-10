package com.backbase.stream.portfolio.config;

import com.backbase.investment.api.service.v1.model.*;
import com.backbase.stream.configuration.InvestmentSagaConfigurationProperties;
import com.backbase.stream.configuration.InvestmentServiceConfiguration;
import com.backbase.stream.investment.ClientUser;
import com.backbase.stream.investment.InvestmentArrangement;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.saga.InvestmentSaga;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.validation.annotation.Validated;

@EnableTask
@Configuration
@AllArgsConstructor
@Slf4j
@Validated
@Import({InvestmentServiceConfiguration.class})
@EnableConfigurationProperties(InvestmentSagaConfigurationProperties.class)
public class SetupPortfolioHierarchyConfiguration {

    private final InvestmentSaga saga;
    private final InvestmentSagaConfigurationProperties properties;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> execute();
    }

    void execute() {
        log.info("Bootstrapping Root Wealth Bundles Structure");

        InvestmentData data = InvestmentData.builder().build();
        data.setInvestmentArrangements(List.of(
            InvestmentArrangement.builder()
                .name("Self-trading")
                .currency("USD")
                .internalId("rnd-eph-self-trading-arr-internal-111222")
                .externalId("rnd-eph-self-trading-arr-111222")
                .externalUserId("rnd-eph-self-trading")
                .productTypeExternalId(ProductTypeEnum.SELF_TRADING.toString())
                .legalEntityExternalIds(List.of("rnd-eph-naomi", "rnd-eph-alex"))
                .build())
        );
        data.setClientUsers(
            List.of(
                ClientUser.builder().externalUserId("rnd-eph-alex")
                    .internalUserId("16122eb4-8aa9-428b-b1e8-d760befbae68")
                    .legalEntityExternalId("rnd-eph-alex")
                    .build(),
                ClientUser.builder().externalUserId("rnd-eph-naomi")
                    .internalUserId("39002c12-db15-44a1-a95c-0b9777e4ff73")
                    .legalEntityExternalId("rnd-eph-naomi")
                    .build()
//                ClientUser.builder().externalUserId("rnd-eph-ddd").internalUserId("ea02c250-87db-4dfc-a6b7-1c0a615a6d89").build(),
//                ClientUser.builder().externalUserId("rnd-eph-bbbbb").internalUserId("ea02c250-87db-4dfc-a6b7-1c0a615a6d90").build()
        ));
        data.setMarkets(getMarkets());
        data.setAssets(getAssets());
        saga.executeTask(new InvestmentTask(
            UUID.randomUUID().toString(),
            data
        )).block();
        log.info("Finished bootstrapping Wealth Bundles Structure");
    }

    private List<Asset> getAssets() {
        return List.of(
                new Asset().name("Nucoro Inc.").isin("NUC012345678").ticker("NUC").status(StatusA10Enum.ACTIVE)
                        .market("XLON").currency("CHF").extraData(Map.of("founded_year", "2008")),
                new Asset().name("Backbase Corp.").isin("BAC987654325").ticker("BAC").status(StatusA10Enum.ACTIVE)
                        .market("XETR").currency("EUR").assetType(AssetTypeEnum.STOCK)
        );
    }

    private List<Market> getMarkets() {
        return List.of(
                new Market().code("XETR").name("Xetra").timeZone(TimeZoneEnum.EUROPE_BERLIN).sessionStart("09:00:00").sessionEnd("18:00:00"),
                new Market().code("XSWX").name("SIX").timeZone(TimeZoneEnum.UTC).sessionStart("09:30:00").sessionEnd("17:00:00"),
                new Market().code("XLON").name("LSE").timeZone(TimeZoneEnum.UTC).sessionStart("09:00:00").sessionEnd("17:30:00")
        );
    }
}
