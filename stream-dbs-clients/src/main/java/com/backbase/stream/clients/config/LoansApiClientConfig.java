package com.backbase.stream.clients.config;

import com.backbase.loan.inbound.api.service.ApiClient;
import com.backbase.loan.inbound.api.service.v2.LoansApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("backbase.communication.services.loan")
public class LoansApiClientConfig extends CompositeApiClientConfig {

    public static final String LOAN_SERVICE_ID = "loan";

    public LoansApiClientConfig() {
        super(LOAN_SERVICE_ID);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiClient loanApiClient(ObjectMapper objectMapper, DateFormat dateFormat) {
        return new ApiClient(getWebClient(), objectMapper, dateFormat)
            .setBasePath(createBasePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public LoansApi loansApi(ApiClient loanApiClient) {
        return new LoansApi(loanApiClient);
    }

}
