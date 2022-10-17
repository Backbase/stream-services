package com.backbase.stream.portfolio.util;

import org.springframework.util.ResourceUtils;
import com.backbase.stream.portfolio.model.WealthAssetBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioAllocationsBundle;
import com.backbase.stream.portfolio.model.WealthPortfolioBundle;
import com.backbase.stream.portfolio.model.WealthRegionsBundle;
import com.backbase.stream.portfolio.model.WealthSubPortfolioBundle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * PortfolioTestUtil.
 * 
 * @author Vladimir Kirchev
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PortfolioTestUtil {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static WealthRegionsBundle getWealthRegionsBundle() throws Exception {
        return getObjectFromJsonFile("classpath:json/regions.json", WealthRegionsBundle.class);
    }

    public static WealthPortfolioAllocationsBundle getWealthPortfolioAllocationsBundle() throws Exception {
        return getObjectFromJsonFile("classpath:json/portfolio-allocations.json", WealthPortfolioAllocationsBundle.class);
    }

    public static WealthAssetBundle getWealthAssetBundle() throws Exception {
        return getObjectFromJsonFile("classpath:json/asset-classes.json", WealthAssetBundle.class);
    }

    public static WealthPortfolioBundle getWealthPortfolioBundle() throws Exception {
        return getObjectFromJsonFile("classpath:json/portfolios.json", WealthPortfolioBundle.class);
    }

    public static WealthSubPortfolioBundle getWealthSubPortfolioBundle() throws Exception {
        return getObjectFromJsonFile("classpath:json/sub-portfolios.json", WealthSubPortfolioBundle.class);
    }

    private static <T> T getObjectFromJsonFile(String fileName, Class<T> type) throws Exception {
        return OBJECT_MAPPER.readValue(ResourceUtils.getFile(fileName), type);
    }

    private static <T> T getObjectFromJsonFile(String fileName, TypeReference<T> type) throws Exception {
        return OBJECT_MAPPER.readValue(ResourceUtils.getFile(fileName), type);
    }
}
