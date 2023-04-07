package com.backbase.stream.openapi;

import static org.springframework.util.AntPathMatcher.DEFAULT_PATH_SEPARATOR;

/** Constants used by OpenAPI Resource. */
public final class Constants {
    public static final String DEFAULT_API_DOCS_URL = "/openapi.yaml";
    public static final String DEFAULT_WEB_JARS_PREFIX_URL = "/webjars";
    public static final String API_DOCS_URL =
            "${backbase.api-docs.path:#{T(com.backbase.stream.openapi.Constants).DEFAULT_API_DOCS_URL}}";
    public static final String WEB_JARS_PREFIX_URL =
            "${backbase.webjars.prefix:#{T(com.backbase.stream.openapi.Constants).DEFAULT_WEB_JARS_PREFIX_URL}}";
    public static final String SWAGGER_UI_URL = "/swagger-ui/index.html?url=";
    public static final String DEFAULT_VALIDATOR_URL = "&validatorUrl=";
    public static final String DEFAULT_SWAGGER_UI_PATH = DEFAULT_PATH_SEPARATOR + "swagger-ui.html";
    public static final String SWAGGER_UI_PATH =
            "${backbase.swagger-ui.path:#{T(com.backbase.stream.openapi.Constants).DEFAULT_SWAGGER_UI_PATH}}";

    private Constants() {
        super();
    }
}
