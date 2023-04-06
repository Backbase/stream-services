package com.backbase.stream.cursor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Payment Created Event.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent extends AbstractDbsEvent {

    private PaymentMessage paymentOrder;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentMessage implements Serializable {

        @JsonProperty("createdBy")
        private String externalUserId;
        @JsonProperty("id")
        private String paymentId;
        private Date createdAt;

    }

}
