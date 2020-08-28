package com.backbase.stream.config.configuration;

import com.backbase.stream.config.repository.KubernetesEnvironmentRepository;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.kubernetes.KubernetesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KubernetesAutoConfiguration.class)
@AllArgsConstructor
public class KubernetesEnvironmentRepositoryConfiguration {

    private final KubernetesClient kubernetesClient;

    @Bean
    public EnvironmentRepository getKubernetesEnvironmentRepository() {
        return new KubernetesEnvironmentRepository(kubernetesClient);
    }

}
