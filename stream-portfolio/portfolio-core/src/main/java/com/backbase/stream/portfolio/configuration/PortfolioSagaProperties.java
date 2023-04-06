package com.backbase.stream.portfolio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.backbase.stream.worker.configuration.StreamWorkerConfiguration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Portfolio Saga Properties.
 *
 * @author Vladimir Kirchev
 *
 */
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("backbase.stream.portfolio.sink")
public class PortfolioSagaProperties extends StreamWorkerConfiguration {

}
