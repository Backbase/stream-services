package com.backbase.stream.transaction;

import com.backbase.dbs.transaction.api.service.v2.model.TransactionState;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsQuery {
    private BigDecimal amountGreaterThan;
    private BigDecimal amountLessThan;
    private String bookingDateGreaterThan;
    private String bookingDateLessThan;
    private List<String> types;
    private String description;
    private String reference;
    private List<String> typeGroups;
    private String counterPartyName;
    private String counterPartyAccountNumber;
    private String creditDebitIndicator;
    private List<String> categories;
    private String billingStatus;
    private TransactionState state;
    private String currency;
    private Integer notes;
    private String id;
    private String productId;
    private String arrangementId;
    private List<String> arrangementsIds;
    private Long fromCheckSerialNumber;
    private Long toCheckSerialNumber;
    private List<Long> checkSerialNumbers;
    private String query;
    private Integer from;
    private String cursor;
    private Integer size;
    private String orderBy;
    private String direction;
    private String secDirection;

}
