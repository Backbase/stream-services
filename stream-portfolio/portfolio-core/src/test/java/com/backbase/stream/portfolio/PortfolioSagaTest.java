package com.backbase.stream.portfolio;

import com.backbase.stream.portfolio.model.AggregatePortfolio;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.PortfolioBundle;
import com.backbase.stream.portfolio.model.PositionBundle;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.backbase.stream.portfolio.service.InstrumentIntegrationService;
import com.backbase.stream.portfolio.service.PortfolioIntegrationService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PortfolioSagaTest {

    @Mock
    private PortfolioIntegrationService portfolioIntegrationService;
    @Mock()
    private InstrumentIntegrationService instrumentIntegrationService;

    @Mock
    WealthBundle wealthBundle;

    @InjectMocks
    private PortfolioSaga portfolioSaga;

    @Test
    void executeTask() {
        PortfolioTask streamTask = new PortfolioTask(wealthBundle);

        PositionBundle positionBundle = new PositionBundle();
        RegionBundle regionBundle = new RegionBundle();
        AssetClassBundle assetClassBundle = new AssetClassBundle();
        InstrumentBundle instrumentBundle = new InstrumentBundle();
        PortfolioBundle portfolioBundle = new PortfolioBundle();
        AggregatePortfolio aggregatePortfolio = new AggregatePortfolio();

        Mockito.when(wealthBundle.getRegions()).thenReturn(List.of(regionBundle));
        Mockito.when(wealthBundle.getAssetClasses()).thenReturn(List.of(assetClassBundle));
        Mockito.when(wealthBundle.getInstruments()).thenReturn(List.of(instrumentBundle));
        Mockito.when(wealthBundle.getPortfolios()).thenReturn(List.of(portfolioBundle));
        Mockito.when(wealthBundle.getPositions()).thenReturn(List.of(positionBundle));
        Mockito.when(wealthBundle.getAggregatePortfolios()).thenReturn(List.of(aggregatePortfolio));

        Mockito.when(portfolioIntegrationService.createPosition(positionBundle)).thenReturn(Mono.just(positionBundle));
        Mockito.when(portfolioIntegrationService.createAggregatePortfolio(aggregatePortfolio))
            .thenReturn(Mono.just(aggregatePortfolio));
        Mockito.when(portfolioIntegrationService.createPortfolio(portfolioBundle))
            .thenReturn(Flux.just(portfolioBundle));
        Mockito.when(instrumentIntegrationService.createAssetClass(assetClassBundle)).thenReturn(Flux.empty());
        Mockito.when(instrumentIntegrationService.createInstrument(instrumentBundle))
            .thenReturn(Mono.just(instrumentBundle));
        Mockito.when(instrumentIntegrationService.createRegion(regionBundle)).thenReturn(Flux.empty());

        portfolioSaga.executeTask(streamTask).block();

        Mockito.verify(portfolioIntegrationService).createPosition(positionBundle);
        Mockito.verify(portfolioIntegrationService).createAggregatePortfolio(aggregatePortfolio);
        Mockito.verify(portfolioIntegrationService).createPortfolio(portfolioBundle);
        Mockito.verify(instrumentIntegrationService).createAssetClass(assetClassBundle);
        Mockito.verify(instrumentIntegrationService).createInstrument(instrumentBundle);
        Mockito.verify(instrumentIntegrationService).createRegion(regionBundle);

        InOrder portfolioTaskOrder = Mockito.inOrder(wealthBundle);
        //noinspection ResultOfMethodCallIgnored
        portfolioTaskOrder.verify(wealthBundle).getRegions();
        //noinspection ResultOfMethodCallIgnored
        portfolioTaskOrder.verify(wealthBundle).getAssetClasses();
        //noinspection ResultOfMethodCallIgnored
        portfolioTaskOrder.verify(wealthBundle).getInstruments();
        //noinspection ResultOfMethodCallIgnored
        portfolioTaskOrder.verify(wealthBundle).getPortfolios();
        //noinspection ResultOfMethodCallIgnored
        portfolioTaskOrder.verify(wealthBundle).getPositions();
        //noinspection ResultOfMethodCallIgnored
        portfolioTaskOrder.verify(wealthBundle).getAggregatePortfolios();

    }

    @Test
    void rollBack() {
        PortfolioTask streamTask = Mockito.spy(new PortfolioTask());
        Mono<PortfolioTask> portfolioTaskMono = portfolioSaga.rollBack(streamTask);

        Assertions.assertNotNull(portfolioTaskMono);

        portfolioTaskMono.block();

        Mockito.verify(streamTask, Mockito.never()).getData();
    }

}