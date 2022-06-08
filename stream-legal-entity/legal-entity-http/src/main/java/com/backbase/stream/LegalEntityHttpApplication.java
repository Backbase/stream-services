package com.backbase.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class LegalEntityHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalEntityHttpApplication.class, args);
    }
}

@Configuration
class LegalEntityHttpConfiguration {

    /**
     * To support tracing requests to the services.
     *
     * @return In memory HttpTraceRepository.
     */
    @Bean
    @ConditionalOnExpression("${management.endpoints.enabled-by-default:false} or ${management.trace.http.enabled:false}")
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

}
