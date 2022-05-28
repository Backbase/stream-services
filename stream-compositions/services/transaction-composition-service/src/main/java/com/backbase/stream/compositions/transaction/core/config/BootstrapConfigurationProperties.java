package com.backbase.stream.compositions.transaction.core.config;

import com.backbase.stream.compositions.transaction.model.TransactionsPostRequestBody;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "bootstrap", ignoreInvalidFields = true)
public class BootstrapConfigurationProperties {
    private List<TransactionsPostRequestBody> transactions;
}
