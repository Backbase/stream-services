package com.backbase.stream.productcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application that launched the {@link SetupProductCatalogApplication} and shuts down
 * on completion.
 */
@SpringBootApplication
public class SetupProductCatalogApplication {

    public static void main(String[] args) {
        SpringApplication springApplication =
                new SpringApplication(SetupProductCatalogApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }
}
