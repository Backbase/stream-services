package com.backbase.stream.cursor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Audit Message Event.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditMessagesEvent extends AbstractDbsEvent {

    private List<AuditMessage> auditMessages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuditMessage implements Serializable {

        private String eventCategory;
        private String objectType;
        private String eventAction;
        private String status;
        private String username;
        private String userId;

    }

}
