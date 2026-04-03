package com.backbase.stream.configuration;


import com.backbase.cdp.ingestion.api.service.v1.CdpApi;
import com.backbase.stream.cdp.CdpSaga;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({CdpProperties.class})
@Configuration
public class CdpConfiguration {

    @Bean
    public CdpSaga cdpSaga(
        CdpApi cdpServiceApi,
        CdpProperties cdpProperties
    ) {
        return new CdpSaga(cdpServiceApi, cdpProperties);
    }

}
