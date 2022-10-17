package com.backbase.stream.portfolio.saga.wealth.region;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.WealthRegionsBundle;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;

/**
 * WealthRegionsTask Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class WealthRegionsTaskTest {

    @Test
    void shouldBeProperlyInitialized() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        Assertions.assertNotNull(wealthRegionsTask.getName());

        RegionBundle data = wealthRegionsTask.getData();

        Assertions.assertNotNull(data);
    }

    @Test
    void shouldGetToString() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        String toStringValue = wealthRegionsTask.toString();

        Assertions.assertNotNull(toStringValue);
    }

    @Test
    void shouldNotBeEqual() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle0 = regions.get(0);
        RegionBundle regionBundle1 = regions.get(1);
        WealthRegionsTask wealthRegionsTask0 = new WealthRegionsTask(regionBundle0);
        WealthRegionsTask wealthRegionsTask1 = new WealthRegionsTask(regionBundle1);

        Assertions.assertNotEquals(wealthRegionsTask0, wealthRegionsTask1);
    }

    @Test
    void shouldNotBeEqual_DifferentType() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        Assertions.assertNotEquals(wealthRegionsTask, new Object());
    }

    @Test
    void shouldNotBeEqual_SameData() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle0 = regions.get(0);
        RegionBundle regionBundle1 = regions.get(1);
        WealthRegionsTask wealthRegionsTask0 = new WealthRegionsTask(regionBundle0);
        WealthRegionsTask wealthRegionsTask1 = new WealthRegionsTask(regionBundle1);

        Assertions.assertNotEquals(wealthRegionsTask0, wealthRegionsTask1);
    }

    @Test
    void shouldGetHashCode() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        Assertions.assertNotEquals(0, wealthRegionsTask.hashCode());
    }

    @Test
    void shouldGetData() throws Exception {
        WealthRegionsBundle wealthRegionsBundle = PortfolioTestUtil.getWealthRegionsBundle();
        List<RegionBundle> regions = wealthRegionsBundle.getRegions();
        RegionBundle regionBundle = regions.get(0);
        WealthRegionsTask wealthRegionsTask = new WealthRegionsTask(regionBundle);

        Assertions.assertNotNull(wealthRegionsTask.getData());
    }
}
