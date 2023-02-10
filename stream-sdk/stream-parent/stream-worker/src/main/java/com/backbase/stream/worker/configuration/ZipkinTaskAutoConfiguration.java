package com.backbase.stream.worker.configuration;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.zipkin2.ZipkinProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.ReporterMetrics;
import zipkin2.reporter.Sender;

/**
 * This supersedes ZipkinAutoConfiguration when creating the reporter for submitting zipkin spans as it blocks the
 * existing thread.
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.task.listener.TaskExecutionListener")
@AutoConfigureBefore(name = "org.springframework.cloud.sleuth.autoconfig.zipkin2.ZipkinAutoConfiguration")
@ConditionalOnProperty(value = {"spring.sleuth.enabled", "spring.zipkin.enabled", "spring.sleuth.task.enabled"},
    matchIfMissing = true)
public class ZipkinTaskAutoConfiguration {

    /**
     * Sams as in 'ZipkinAutoConfiguration.reporter' but without the `checkResult` operation which start as new thread
     * that is holding the spring cloud task to finish its execution.
     *
     * @param reporterMetrics .
     * @param zipkin          .
     * @param sender          .
     * @return .
     */
    @Primary
    @Bean("zipkinReporter")
    Reporter<Span> reporter(ReporterMetrics reporterMetrics, ZipkinProperties zipkin,
        @Qualifier("zipkinSender") Sender sender) {

        AsyncReporter<Span> asyncReporter = AsyncReporter.builder(sender).queuedMaxSpans(zipkin.getQueuedMaxSpans())
            .messageTimeout(zipkin.getMessageTimeout(), TimeUnit.SECONDS).metrics(reporterMetrics)
            .build(zipkin.getEncoder());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Flushing remaining spans on shutdown");
            asyncReporter.flush();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(zipkin.getMessageTimeout()) + 500);
                log.debug("Flushing done - closing the reporter");
                asyncReporter.close();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }));

        return asyncReporter;
    }
}
