package com.backbase.streams.tailoredvalue.configuration;

import com.backbase.streams.tailoredvalue.plan.PlansSaga;
import com.backbase.tailoredvalue.planmanager.service.api.v0.PlansApi;
import com.backbase.tailoredvalue.planmanager.service.api.v0.UserPlansApi;
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
    public PlansSaga plansSaga(PlansApi plansApi, UserPlansApi userPlansApi, PlansProperties plansProperties) {
        return new PlansSaga(plansApi, userPlansApi, plansProperties);
    }

}
