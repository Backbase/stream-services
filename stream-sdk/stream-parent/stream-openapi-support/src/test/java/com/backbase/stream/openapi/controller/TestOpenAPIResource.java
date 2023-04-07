package com.backbase.stream.openapi.controller;

import io.swagger.v3.oas.models.OpenAPI;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

@Slf4j
public class TestOpenAPIResource {

    @Test
    public void testOpenAPI() throws FileNotFoundException {
        OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.setOpenApiFile(getClass().getResource("/openapi.yaml").getFile());
        OpenAPI openApi = openApiResource.getOpenApi("http://myserver");

        log.info("OpenAPI: \n{}", OpenApiResource.toYamlString(openApi));
    }
}
