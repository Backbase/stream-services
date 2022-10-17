package com.backbase.stream.portfolio.saga.wealth.allocation;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioAllocationsBundle;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;

/**
 * WealthPortfolioAllocationsTask Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class WealthPortfolioAllocationsTaskTest {
    @Test
    void shouldBeProperlyInitialized() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);

        Assertions.assertNotNull(wealthPortfolioAllocationsTask.getName());

        AllocationBundle data = wealthPortfolioAllocationsTask.getData();

        Assertions.assertNotNull(data);
    }

    @Test
    void shouldGetToString() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);

        String toStringValue = wealthPortfolioAllocationsTask.toString();

        Assertions.assertNotNull(toStringValue);
    }

    @Test
    void shouldNotBeEqual() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle0 = batchPortfolioAllocations.get(0);
        AllocationBundle allocationBundle1 = batchPortfolioAllocations.get(1);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask0 =
                new WealthPortfolioAllocationsTask(allocationBundle0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask1 =
                new WealthPortfolioAllocationsTask(allocationBundle1);

        Assertions.assertNotEquals(wealthPortfolioAllocationsTask0, wealthPortfolioAllocationsTask1);
        Assertions.assertNotEquals(wealthPortfolioAllocationsTask1, wealthPortfolioAllocationsTask0);
    }

    @Test
    void shouldNotBeEqual_DifferentType() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);

        Assertions.assertNotEquals(wealthPortfolioAllocationsTask, new Object());
    }

    @Test
    void shouldNotBeEqual_SameData() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle0 = batchPortfolioAllocations.get(0);
        AllocationBundle allocationBundle1 = batchPortfolioAllocations.get(1);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask0 =
                new WealthPortfolioAllocationsTask(allocationBundle0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask1 =
                new WealthPortfolioAllocationsTask(allocationBundle1);

        Assertions.assertNotEquals(wealthPortfolioAllocationsTask0, wealthPortfolioAllocationsTask1);
        Assertions.assertNotEquals(wealthPortfolioAllocationsTask1, wealthPortfolioAllocationsTask0);
    }

    @Test
    void shouldGetHashCode() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);

        Assertions.assertNotEquals(0, wealthPortfolioAllocationsTask.hashCode());
    }

    @Test
    void shouldGetData() throws Exception {
        WealthPortfolioAllocationsBundle wealthPortfolioAllocationsBundle =
                PortfolioTestUtil.getWealthPortfolioAllocationsBundle();
        List<AllocationBundle> batchPortfolioAllocations =
                wealthPortfolioAllocationsBundle.getBatchPortfolioAllocations();
        AllocationBundle allocationBundle = batchPortfolioAllocations.get(0);
        WealthPortfolioAllocationsTask wealthPortfolioAllocationsTask =
                new WealthPortfolioAllocationsTask(allocationBundle);

        Assertions.assertNotNull(wealthPortfolioAllocationsTask.getData());
    }
}
