package com.backbase.stream.portfolio.util;

import java.util.List;
import com.backbase.stream.portfolio.model.Country;
import com.backbase.stream.portfolio.model.Region;
import com.backbase.stream.portfolio.model.RegionBundle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Region Test Util.
 * 
 * @author Vladimir Kirchev
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegionTestUtil {

    public static RegionBundle createRegionBundleEu() {
        Region regionEu = new Region().name("Europe").code("EU");
        List<Country> countriesEu =
                List.of(new Country().name("Netherlands").code("NL"), new Country().name("Ukraine").code("UA"));
        return new RegionBundle().region(regionEu).countries(countriesEu);
    }

    public static RegionBundle createRegionBundleUs() {
        Region regionUs = new Region().name("USA").code("US");
        List<Country> countriesUs = List.of(new Country().name("USA").code("US"));
        return new RegionBundle().region(regionUs).countries(countriesUs);
    }

    public static List<RegionBundle> createRegionBundles() {
        return List.of(createRegionBundleEu(), createRegionBundleUs());
    }

}
