package com.backbase.stream.portfolio.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backbase.stream.portfolio.PortfolioSaga;
import com.backbase.stream.portfolio.PortfolioTask;
import com.backbase.stream.portfolio.model.WealthBundle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class SetupPortfolioHierarchyConfigurationTest {

    @Mock private PortfolioSaga portfolioSaga;
    @Spy private BootstrapConfigurationProperties bootstrapConfigurationProperties;
    @InjectMocks SetupPortfolioHierarchyConfiguration configuration;

    @Test
    void commandLineRunner() {
        WealthBundle wealthBundle = new WealthBundle();

        when(bootstrapConfigurationProperties.getWealthBundles()).thenReturn(List.of(wealthBundle));
        when(portfolioSaga.executeTask(any(PortfolioTask.class))).thenReturn(Mono.empty());

        configuration.execute();

        verify(portfolioSaga)
                .executeTask(
                        Mockito.argThat(
                                portfolioTask -> portfolioTask.getData().equals(wealthBundle)));
    }

    @Test
    void commandLineRunnerNoData() {

        configuration.execute();

        verify(portfolioSaga, never()).executeTask(any());
    }
}
