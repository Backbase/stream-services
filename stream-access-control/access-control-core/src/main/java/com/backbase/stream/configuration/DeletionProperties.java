package com.backbase.stream.configuration;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("backbase.stream.dbs.deletion")
public class DeletionProperties {

    /** The function group item type to delete. */
    private FunctionGroupItemType functionGroupItemType = FunctionGroupItemType.NONE;

    public enum FunctionGroupItemType {
        NONE,
        TEMPLATE
    }
}
