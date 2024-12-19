package com.backbase.stream.configuration;

import com.backbase.audiences.collector.api.service.v1.HandlersServiceApi;
import com.backbase.stream.audiences.UserKindSegmentationSaga;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({UserKindSegmentationProperties.class})
@Configuration
public class AudiencesSegmentationConfiguration {

  @Bean
  public UserKindSegmentationSaga userKindSegmentationSaga(
      HandlersServiceApi handlersServiceApi,
      UserKindSegmentationProperties userKindSegmentationProperties) {
    return new UserKindSegmentationSaga(handlersServiceApi, userKindSegmentationProperties);
  }
}
