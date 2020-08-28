package com.backbase.stream.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;

public class StreamMicrometerCommonTags {

    @Value("${spring.cloud.dataflow.stream.name:unknown}")
    private String streamName;

    @Value("${spring.cloud.dataflow.stream.app.label:unknown}")
    private String applicationName;

    @Value("${spring.cloud.stream.instanceIndex:0}")
    private String instanceIndex;

    @Value("${spring.cloud.application.guid:unknown}")
    private String applicationGuid;

    @Value("${spring.cloud.dataflow.stream.app.type:unknown}")
    private String applicationType;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("stream.name", streamName)
            .commonTags("application.name", applicationName)
            .commonTags("application.type", applicationType)
            .commonTags("instance.index", instanceIndex)
            .commonTags("application.guid", applicationGuid);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> renameNameTag() {
        return registry -> {
            if (registry.getClass().getCanonicalName().contains("AtlasMeterRegistry")) {
                registry.config().meterFilter(MeterFilter.renameTag("spring.integration", "name", "aname"));
            }
            if (registry.getClass().getCanonicalName().contains("InfluxMeterRegistry")) {
                registry.config().meterFilter(MeterFilter.replaceTagValues("application.name",
                    tagValue -> ("time".equalsIgnoreCase(tagValue)) ? "atime" : tagValue));
            }
        };
    }
}
