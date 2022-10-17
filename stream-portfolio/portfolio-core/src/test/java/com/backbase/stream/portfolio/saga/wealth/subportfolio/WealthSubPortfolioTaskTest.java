package com.backbase.stream.portfolio.saga.wealth.subportfolio;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthSubPortfolioBundle;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;

/**
 * WealthSubPortfolioTask Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class WealthSubPortfolioTaskTest {
    @Test
    void shouldBeProperlyInitialized() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        Assertions.assertNotNull(wealthSubPortfolioTask.getName());

        SubPortfolioBundle data = wealthSubPortfolioTask.getData();

        Assertions.assertNotNull(data);
    }

    @Test
    void shouldGetToString() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        String toStringValue = wealthSubPortfolioTask.toString();

        Assertions.assertNotNull(toStringValue);
    }

    @Test
    void shouldNotBeEqual() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);
        SubPortfolioBundle subPortfolioBundle1 = batchSubPortfolios.get(1);
        WealthSubPortfolioTask wealthSubPortfolioTask0 = new WealthSubPortfolioTask(subPortfolioBundle0);
        WealthSubPortfolioTask wealthSubPortfolioTask1 = new WealthSubPortfolioTask(subPortfolioBundle1);

        Assertions.assertNotEquals(wealthSubPortfolioTask0, wealthSubPortfolioTask1);
        Assertions.assertNotEquals(wealthSubPortfolioTask1, wealthSubPortfolioTask0);
    }

    @Test
    void shouldBeEqual_SameInstance() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask0 = new WealthSubPortfolioTask(subPortfolioBundle0);

        Assertions.assertEquals(wealthSubPortfolioTask0, wealthSubPortfolioTask0);
    }

    @Test
    void shouldNotBeEqual_DifferentType() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        Assertions.assertNotEquals(wealthSubPortfolioTask, new Object());
    }

    @Test
    void shouldNotBeEqual_SameData() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle0 = batchSubPortfolios.get(0);
        SubPortfolioBundle subPortfolioBundle1 = batchSubPortfolios.get(1);
        WealthSubPortfolioTask wealthSubPortfolioTask0 = new WealthSubPortfolioTask(subPortfolioBundle0);
        WealthSubPortfolioTask wealthSubPortfolioTask1 = new WealthSubPortfolioTask(subPortfolioBundle1);

        Assertions.assertNotEquals(wealthSubPortfolioTask0, wealthSubPortfolioTask1);
        Assertions.assertNotEquals(wealthSubPortfolioTask1, wealthSubPortfolioTask0);
    }

    @Test
    void shouldGetHashCode() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        Assertions.assertNotEquals(0, wealthSubPortfolioTask.hashCode());
    }

    @Test
    void shouldGetData() throws Exception {
        WealthSubPortfolioBundle wealthSubPortfolioBundle = PortfolioTestUtil.getWealthSubPortfolioBundle();
        List<SubPortfolioBundle> batchSubPortfolios = wealthSubPortfolioBundle.getBatchSubPortfolios();
        SubPortfolioBundle subPortfolioBundle = batchSubPortfolios.get(0);
        WealthSubPortfolioTask wealthSubPortfolioTask = new WealthSubPortfolioTask(subPortfolioBundle);

        Assertions.assertNotNull(wealthSubPortfolioTask.getData());
    }
}
