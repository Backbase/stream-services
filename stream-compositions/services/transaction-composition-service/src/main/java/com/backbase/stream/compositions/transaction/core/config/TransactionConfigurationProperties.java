package com.backbase.stream.compositions.transaction.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.compositions.transaction")
public class TransactionConfigurationProperties {
    private Events events;
    private Cursor cursor;
    private Integer defaultStartOffsetInDays;

    @Data
    @NoArgsConstructor
    public static class Events {
        private Boolean enableCompleted = Boolean.FALSE;
        private Boolean enableFailed = Boolean.FALSE;
    }

    @Data
    @NoArgsConstructor
    public static class Cursor {
        private Boolean enabled = Boolean.FALSE;
        private Boolean transactionIdsFilterEnabled = Boolean.FALSE;
    }

    public final boolean isCursorEnabled() {
        return Boolean.TRUE.equals(cursor.getEnabled());
    }

    public final boolean isTransactionIdsFilterEnabled() {
        return Boolean.TRUE.equals(cursor.getTransactionIdsFilterEnabled());
    }
}
