package com.backbase.stream.accesscontrol.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@SpringBootApplication
public class ExportBusinessFunctionsToYaml {

    public static void main(String[] args) {
        SpringApplication.run(ExportBusinessFunctionsToYaml.class, args);
    }

}

@Service
@RequiredArgsConstructor
@Slf4j
class Exporter implements ApplicationListener<ContextRefreshedEvent> {

    private final BusinessFunctionRepository repository;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        List<BusinessFunction> block = repository.findAll().collectList().block();
        Map<String, List<BusinessFunction>> collect = block.stream().collect(Collectors.groupingBy(BusinessFunction::getResourceName));

        File parent = new File("target/export");
        parent.mkdirs();

        collect.forEach((resource, list) -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", "Manage " + resource);
            result.put("functions", list);

            try {
                String yamlString = objectMapper.writeValueAsString(result);
                Files.write(new File(parent, resource + ".yaml").toPath(), yamlString.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}


@Repository
interface BusinessFunctionRepository extends ReactiveCrudRepository<BusinessFunction, String> {

}


@Data
@Table("business_function")
class BusinessFunction {

    @Id
    private String id;
    @Column("function_code")
    private String functionCode;
    @Column("function_name")
    private String functionName;
    @Column("resource_code")
    private String resourceCode;
    @Column("resource_name")
    private String resourceName;

    @Transient
    private List<Privilege> privileges = Arrays.asList(new Privilege("view"), new Privilege("create"), new Privilege("edit"), new Privilege("delete"), new Privilege("approve"));

}

@Getter
@RequiredArgsConstructor
class Privilege {

    private final String privilege;
}
