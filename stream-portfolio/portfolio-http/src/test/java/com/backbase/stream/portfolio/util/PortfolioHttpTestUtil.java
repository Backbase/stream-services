package com.backbase.stream.portfolio.util;

import java.util.List;
import org.springframework.util.ResourceUtils;
import com.backbase.stream.portfolio.model.AllocationBundle;
import com.backbase.stream.portfolio.model.AssetClassBundle;
import com.backbase.stream.portfolio.model.HierarchyBundle;
import com.backbase.stream.portfolio.model.InstrumentBundle;
import com.backbase.stream.portfolio.model.Portfolio;
import com.backbase.stream.portfolio.model.RegionBundle;
import com.backbase.stream.portfolio.model.SubPortfolioBundle;
import com.backbase.stream.portfolio.model.TransactionCategory;
import com.backbase.stream.portfolio.model.ValuationsBundle;
import com.backbase.stream.portfolio.model.WealthBundle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * PortfolioHttpTest Util.
 * 
 * @author Vladimir Kirchev
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PortfolioHttpTestUtil {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static final String X_TID_HEADER_NAME = "X-TID";
    public static final String X_TID_HEADER_VALUE = "tenant-id";

    public static List<RegionBundle> getRegionBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/region-bundles.json", new TypeReference<List<RegionBundle>>() {});
    }

    public static List<WealthBundle> getWealthBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/wealth-bundles.json", new TypeReference<List<WealthBundle>>() {});
    }

    public static List<AllocationBundle> getAllocationBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/wealth-portfolio-allocations.json",
                new TypeReference<List<AllocationBundle>>() {});
    }

    public static List<AssetClassBundle> getAssetClasseBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/asset-classes.json",
                new TypeReference<List<AssetClassBundle>>() {});
    }

    public static List<Portfolio> getPortfolios() throws Exception {
        return getObjectFromJsonFile("classpath:json/portfolios.json", new TypeReference<List<Portfolio>>() {});
    }

    public static List<SubPortfolioBundle> getSubPortfolios() throws Exception {
        return getObjectFromJsonFile("classpath:json/sub-portfolios.json",
                new TypeReference<List<SubPortfolioBundle>>() {});
    }

    public static List<InstrumentBundle> getInstrumentBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/instruments.json", new TypeReference<List<InstrumentBundle>>() {});
    }

    public static List<TransactionCategory> getTransactionCategories() throws Exception {
        return getObjectFromJsonFile("classpath:json/transaction-categories.json",
                new TypeReference<List<TransactionCategory>>() {});
    }

    public static List<ValuationsBundle> getValuationsBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/valuations.json",
                new TypeReference<List<ValuationsBundle>>() {});
    }
    
    public static List<HierarchyBundle> getHierarchyBundles() throws Exception {
        return getObjectFromJsonFile("classpath:json/hierarchies.json", new TypeReference<List<HierarchyBundle>>() {});
    }

    private static <T> T getObjectFromJsonFile(String fileName, Class<T> type) throws Exception {
        return OBJECT_MAPPER.readValue(ResourceUtils.getFile(fileName), type);
    }

    private static <T> T getObjectFromJsonFile(String fileName, TypeReference<T> type) throws Exception {
        return OBJECT_MAPPER.readValue(ResourceUtils.getFile(fileName), type);
    }
}
