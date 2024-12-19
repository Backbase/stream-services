package com.backbase.streams.tailoredvalue.configuration;

import com.backbase.streams.tailoredvalue.PlansService;
import com.backbase.tailoredvalue.planmanager.service.api.v1.PlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v1.UserPlansApi;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    PlansProperties.class
})
@AllArgsConstructor
@Configuration
public class PlanServiceConfiguration {

    @Bean
    public PlansService plansService(PlansApi plansApi, UserPlansApi userPlansApi, PlansProperties plansProperties) {
        return new PlansService(plansApi, userPlansApi, plansProperties);
    }

}
