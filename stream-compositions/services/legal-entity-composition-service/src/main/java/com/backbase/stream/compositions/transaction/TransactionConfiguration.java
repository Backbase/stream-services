package com.backbase.stream.compositions.transaction;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backbase.stream.compositions.transaction")
public class TransactionConfiguration {
}
