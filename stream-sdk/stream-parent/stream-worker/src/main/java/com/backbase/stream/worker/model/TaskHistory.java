package com.backbase.stream.worker.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskHistory {

    private OffsetDateTime timestamp;

    private String entity;
    private String internalId;
    private String externalId;

    private String operation;
    private String result;

    private String message;
    private String errorMessage;
    private String resolution;
    private Severity severity;

    public enum Severity {
        INFO,
        WARN,
        ERROR
    }

    /**
     * To String method.
     *
     * @return A human-readable string
     */
    public String toDisplayString() {
        String entityType = entity != null ? entity.toLowerCase() : "";
        if (errorMessage == null) {
            return String.format(
                    "%s [%s] [%s] [%s] %s - %s",
                    timestamp,
                    severity,
                    String.format("%1$25s", entityType),
                    String.format("%1$15s", operation),
                    externalId,
                    message);
        } else {
            return String.format(
                    "%s [%s] [%s] [%s] %s - %s: Error Message: %s",
                    timestamp,
                    severity,
                    String.format("%1$25s", entityType),
                    String.format("%1$15s", operation),
                    externalId,
                    message,
                    errorMessage);
        }
    }
}
