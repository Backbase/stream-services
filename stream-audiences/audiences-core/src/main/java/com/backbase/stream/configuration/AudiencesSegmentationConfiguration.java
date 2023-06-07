package com.backbase.stream.configuration;

import com.backbase.stream.audiences.CustomersSegmentSaga;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AllArgsConstructor
@Configuration
public class AudiencesSegmentationConfiguration {

    @Bean
    public CustomersSegmentSaga userKindSegmentationSaga() {
        return new CustomersSegmentSaga();
    }

}
