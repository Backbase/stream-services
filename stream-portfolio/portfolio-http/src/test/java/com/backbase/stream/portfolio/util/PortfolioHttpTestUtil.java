package com.backbase.stream.portfolio.util;

import java.util.List;

import org.springframework.util.ResourceUtils;

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

	public static List<WealthBundle> getWealthBundles() throws Exception {
		return getObjectFromJsonFile("classpath:json/wealth-bundles.json", new TypeReference<List<WealthBundle>>() {
		});
	}

	private static <T> T getObjectFromJsonFile(String fileName, Class<T> type) throws Exception {
		return OBJECT_MAPPER.readValue(ResourceUtils.getFile(fileName), type);
	}

	private static <T> T getObjectFromJsonFile(String fileName, TypeReference<T> type) throws Exception {
		return OBJECT_MAPPER.readValue(ResourceUtils.getFile(fileName), type);
	}
}
