package com.backbase.stream.jobprofile;

import com.backbase.stream.configuration.AccessControlConfiguration;
import com.backbase.stream.service.BusinessFunctionService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Export available business function templates and write to files grouped per business function groups
 */
@Configuration
@Import({AccessControlConfiguration.class})
@Slf4j
@AllArgsConstructor
class ExportJobProfileTemplates {

    private final BusinessFunctionService businessFunctionService;

    /**
     * The Command Line Runner is invoked when Spring Boot application starts and shutdown on completion.
     *
     * @return Configured Application Runner
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {

            File exportDirectory = new File("export");
            exportDirectory.mkdirs();

            ObjectMapper mapper = new ObjectMapper((new YAMLFactory()));
            mapper.registerModule(new JavaTimeModule());
            mapper.findAndRegisterModules();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            businessFunctionService.getAllBusinessFunctions().collectList().block().forEach(businessFunctionGroup -> {
                try {
                    mapper.writeValue(new File(exportDirectory, businessFunctionGroup.getId() + ".yaml"), businessFunctionGroup);
                } catch (IOException e) {
                    log.error("Failed to export businessGroup: " + businessFunctionGroup.getName(), e);
                }
            });
        };
    }

}
