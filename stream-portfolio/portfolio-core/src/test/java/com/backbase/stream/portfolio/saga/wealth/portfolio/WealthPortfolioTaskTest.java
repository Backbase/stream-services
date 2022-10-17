package com.backbase.stream.portfolio.saga.wealth.portfolio;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.WealthPortfolioBundle;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;

/**
 * WealthPortfolioTask Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class WealthPortfolioTaskTest {
    @Test
    void shouldBeProperlyInitialized() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        Assertions.assertNotNull(wealthPortfolioTask.getName());

        Portfolio data = wealthPortfolioTask.getData();

        Assertions.assertNotNull(data);
    }

    @Test
    void shouldGetToString() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        String toStringValue = wealthPortfolioTask.toString();

        Assertions.assertNotNull(toStringValue);
    }

    @Test
    void shouldNotBeEqual() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio0 = portfolios.get(0);
        Portfolio portfolio1 = portfolios.get(1);
        WealthPortfolioTask wealthPortfolioTask0 = new WealthPortfolioTask(portfolio0);
        WealthPortfolioTask wealthPortfolioTask1 = new WealthPortfolioTask(portfolio1);

        Assertions.assertNotEquals(wealthPortfolioTask0, wealthPortfolioTask1);
        Assertions.assertNotEquals(wealthPortfolioTask1, wealthPortfolioTask0);
    }

    @Test
    void shouldNotBeEqual_DifferentType() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        Assertions.assertNotEquals(wealthPortfolioTask, new Object());
    }

    @Test
    void shouldNotBeEqual_SameData() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio0 = portfolios.get(0);
        Portfolio portfolio1 = portfolios.get(1);
        WealthPortfolioTask wealthPortfolioTask0 = new WealthPortfolioTask(portfolio0);
        WealthPortfolioTask wealthPortfolioTask1 = new WealthPortfolioTask(portfolio1);

        Assertions.assertNotEquals(wealthPortfolioTask0, wealthPortfolioTask1);
        Assertions.assertNotEquals(wealthPortfolioTask1, wealthPortfolioTask0);
    }

    @Test
    void shouldGetHashCode() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        Assertions.assertNotEquals(0, wealthPortfolioTask.hashCode());
    }

    @Test
    void shouldGetData() throws Exception {
        WealthPortfolioBundle wealthPortfolioBundle = PortfolioTestUtil.getWealthPortfolioBundle();
        List<Portfolio> portfolios = wealthPortfolioBundle.getPortfolios();
        Portfolio portfolio = portfolios.get(0);
        WealthPortfolioTask wealthPortfolioTask = new WealthPortfolioTask(portfolio);

        Assertions.assertNotNull(wealthPortfolioTask.getData());
    }
}
