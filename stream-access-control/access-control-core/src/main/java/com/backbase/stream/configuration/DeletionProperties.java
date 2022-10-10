package com.backbase.stream.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties("backbase.stream.dbs.deletion")
public class DeletionProperties {

    /**
     * The function group item type to delete.
     */
    private FunctionGroupItemType functionGroupItemType = FunctionGroupItemType.NONE;

    public enum FunctionGroupItemType {
        NONE,
        TEMPLATE
    }
}
