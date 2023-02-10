package com.backbase.stream.worker.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forces calling `ZipkinAutoConfiguration.cleanup` method to stop its executor service from preventing the application
 * to shut down when finishing the execution of spring cloud tasks.
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.task.listener.TaskExecutionListener")
@ConditionalOnProperty(value = {"spring.sleuth.enabled", "spring.zipkin.enabled", "spring.sleuth.task.enabled"},
    matchIfMissing = true)
public class TaskZipkinLifecycleAutoConfiguration {

    static final String ZIPKIN_CONFIGURATION_BEAN = "org.springframework.cloud.sleuth.autoconfig.zipkin2.ZipkinAutoConfiguration";

    /**
     * Listener to the ApplicationReadyEvent once it is published at the end of the spring cloud task execution.
     *
     * @param postProcessor .
     * @return .
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyListener(
        InitDestroyAnnotationBeanPostProcessor postProcessor) {
        return event -> {
            var applicationContext = event.getApplicationContext();
            if (applicationContext.containsBeanDefinition(ZIPKIN_CONFIGURATION_BEAN)) {
                var zipkinConfig = applicationContext.getBean(ZIPKIN_CONFIGURATION_BEAN);
                postProcessor.postProcessBeforeDestruction(zipkinConfig, ZIPKIN_CONFIGURATION_BEAN);
            }
        };
    }
}
