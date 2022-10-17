package com.backbase.stream.portfolio.saga.wealth.asset;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.WealthAssetBundle;
import com.backbase.stream.portfolio.util.PortfolioTestUtil;

/**
 * WealthAssetsTask Test.
 * 
 * @author Vladimir Kirchev
 *
 */
class WealthAssetsTaskTest {
    @Test
    void shouldBeProperlyInitialized() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        Assertions.assertNotNull(wealthAssetsTask.getName());

        AssetClassBundle data = wealthAssetsTask.getData();

        Assertions.assertNotNull(data);
    }

    @Test
    void shouldGetToString() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        String toStringValue = wealthAssetsTask.toString();

        Assertions.assertNotNull(toStringValue);
    }

    @Test
    void shouldNotBeEqual() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle0 = assetClasses.get(0);
        AssetClassBundle assetClassBundle1 = assetClasses.get(1);
        WealthAssetsTask wealthAssetsTask0 = new WealthAssetsTask(assetClassBundle0);
        WealthAssetsTask wealthAssetsTask1 = new WealthAssetsTask(assetClassBundle1);

        Assertions.assertNotEquals(wealthAssetsTask0, wealthAssetsTask1);
        Assertions.assertNotEquals(wealthAssetsTask1, wealthAssetsTask0);
    }
    
    @Test
    void shouldBeEqual_SameInstance() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle0 = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask0 = new WealthAssetsTask(assetClassBundle0);
        
        Assertions.assertEquals(wealthAssetsTask0, wealthAssetsTask0);
    }

    @Test
    void shouldNotBeEqual_DifferentType() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        Assertions.assertNotEquals(wealthAssetsTask, new Object());
    }

//    @Test
//    void shouldNotBeEqual_SameData() throws Exception {
//        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
//        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
//        AssetClassBundle assetClassBundle0 = assetClasses.get(0);
//        AssetClassBundle assetClassBundle1 = assetClasses.get(1);
//        WealthAssetsTask wealthAssetsTask0 = new WealthAssetsTask(assetClassBundle0);
//        WealthAssetsTask wealthAssetsTask1 = new WealthAssetsTask(assetClassBundle1);
//
//        Assertions.assertNotEquals(wealthAssetsTask0, wealthAssetsTask1);
//        Assertions.assertNotEquals(wealthAssetsTask1, wealthAssetsTask0);
//    }

    @Test
    void shouldGetHashCode() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        Assertions.assertNotEquals(0, wealthAssetsTask.hashCode());
    }

    @Test
    void shouldGetData() throws Exception {
        WealthAssetBundle wealthAssetBundle = PortfolioTestUtil.getWealthAssetBundle();
        List<AssetClassBundle> assetClasses = wealthAssetBundle.getAssetClasses();
        AssetClassBundle assetClassBundle = assetClasses.get(0);
        WealthAssetsTask wealthAssetsTask = new WealthAssetsTask(assetClassBundle);

        Assertions.assertNotNull(wealthAssetsTask.getData());
    }
}
