package com.backbase.stream.portfolio.config;

import com.backbase.stream.configuration.InvestmentSagaConfigurationProperties;
import com.backbase.stream.configuration.InvestmentServiceConfiguration;
import com.backbase.stream.investment.ClientUser;
import com.backbase.stream.investment.InvestmentData;
import com.backbase.stream.investment.InvestmentTask;
import com.backbase.stream.investment.saga.InvestmentSaga;
import java.util.List;
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

        InvestmentData data = new InvestmentData();
        data.setClientUsers(
            List.of(
                ClientUser.builder().externalUserId("rndwlt-eph-alex").internalUserId("16122eb4-8aa9-428b-b1e8-d760befbae68").build(),
                ClientUser.builder().externalUserId("rndwlt-eph-naomi").internalUserId("39002c12-db15-44a1-a95c-0b9777e4ff73").build(),
                ClientUser.builder().externalUserId("rndwlt-eph-ddd").internalUserId("ea02c250-87db-4dfc-a6b7-1c0a615a6d89").build(),
                ClientUser.builder().externalUserId("rndwlt-eph-bbbbb").internalUserId("ea02c250-87db-4dfc-a6b7-1c0a615a6d90").build()
        ));
        saga.executeTask(new InvestmentTask(
            UUID.randomUUID().toString(),
            data
        )).block();
        log.info("Finished bootstrapping Wealth Bundles Structure");
    }

}
