package com.backbase.stream.openapi.controller;

import static com.backbase.stream.openapi.Constants.API_DOCS_URL;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Open API Resource that returns an OpenAPI document with the correct Server URL already filled in.
 */
@RestController
@Slf4j
public class OpenApiResource {

    @Value("${backbase.openapi.location:META-INF/openapi/openapi.yaml}")
    private String openApiFile;

    /**
     * Open API Document with transformed server url.
     *
     * @param serverHttpRequest The current request
     * @param apiDocsUrl        The URL the openapi document is served on
     * @return Transformed YAML representation of the OpenAPI document
     * @throws FileNotFoundException thrown if Open API document is not found.
     */
    @GetMapping(value = API_DOCS_URL, produces = "application/vnd.oai.openapi")
    public Mono<String> getOpenApi(ServerHttpRequest serverHttpRequest, @Value(API_DOCS_URL) String apiDocsUrl)
        throws FileNotFoundException {
        String serverUrl = calculateServerUrl(serverHttpRequest, apiDocsUrl);
        OpenAPI openAPI = this.getOpenApi(serverUrl);
        return Mono.just(toYamlString(openAPI));
    }

    protected OpenAPI getOpenApi(String serverUrl) throws FileNotFoundException {

        File file = ResourceUtils.getFile(openApiFile);

        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        SwaggerParseResult swaggerParseResult = openAPIParser
            .readLocation(file.toString(), new ArrayList<>(), parseOptions);

        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Server e = new Server();
        e.setUrl(serverUrl);
        e.setDescription("Server calculated by Apollo");

        if (openAPI.getServers() == null) {
            openAPI.setServers(new ArrayList<>());
        }
        openAPI.getServers().add(0, e);

        return openAPI;
    }


    private String calculateServerUrl(ServerHttpRequest serverHttpRequest, String apiDocsUrl) {
        String requestUrl = serverHttpRequest.getURI().toString();
        String serverBaseUrl = requestUrl.substring(0, requestUrl.length() - apiDocsUrl.length());
        log.debug("calculated server url: {}", serverBaseUrl);
        return serverBaseUrl;
    }


    protected static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }
        SimpleModule module = new SimpleModule("OpenAPIModule");
        module.addSerializer(OpenAPI.class, new OpenAPISerializer());
        try {
            ObjectMapper mapper = Yaml.mapper()
                .registerModule(module)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);

            YAMLFactory factory = (YAMLFactory) mapper.getFactory();
            factory.disable(YAMLGenerator.Feature.MINIMIZE_QUOTES);

            return mapper.writeValueAsString(openAPI);

        } catch (Exception e) {
            System.err.println("Can not create yaml content");
        }
        return null;
    }


    private static class OpenAPISerializer extends JsonSerializer<OpenAPI> {

        @Override
        public void serialize(OpenAPI value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeStartObject();
                gen.writeStringField("openapi", value.getOpenapi());
                if (value.getInfo() != null) {
                    gen.writeObjectField("info", value.getInfo());
                }
                if (value.getExternalDocs() != null) {
                    gen.writeObjectField("externalDocs", value.getExternalDocs());
                }
                if (value.getServers() != null) {
                    gen.writeObjectField("servers", value.getServers());
                }
                if (value.getSecurity() != null) {
                    gen.writeObjectField("security", value.getSecurity());
                }
                if (value.getTags() != null) {
                    gen.writeObjectField("tags", value.getTags());
                }
                if (value.getPaths() != null) {
                    gen.writeObjectField("paths", value.getPaths());
                }
                if (value.getComponents() != null) {
                    gen.writeObjectField("components", value.getComponents());
                }
                if (value.getExtensions() != null) {
                    for (Map.Entry<String, Object> e : value.getExtensions().entrySet()) {
                        gen.writeObjectField(e.getKey(), e.getValue());
                    }
                }
                gen.writeEndObject();
            }
        }
    }

    public void setOpenApiFile(String openApiFile) {
        this.openApiFile = openApiFile;
    }
}
