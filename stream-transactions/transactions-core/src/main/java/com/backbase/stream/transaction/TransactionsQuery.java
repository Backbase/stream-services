package com.backbase.stream.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionsQuery {
    private BigDecimal amountGreaterThan;
    private BigDecimal amountLessThan;
    private String bookingDateGreaterThan;
    private String bookingDateLessThan;
    private String type;
    private List<String> types;
    private String description;
    private String reference;
    private String typeGroup;
    private List<String> typeGroups;
    private String counterPartyName;
    private String counterPartyAccountNumber;
    private String creditDebitIndicator;
    private String category;
    private List<String> categories;
    private String billingStatus;
    private String state;
    private String currency;
    private Integer notes;
    private String id;
    private String productId;
    private String arrangementId;
    private List<String> arrangementsIds;
    private BigDecimal fromCheckSerialNumber;
    private BigDecimal toCheckSerialNumber;
    private List<BigDecimal> checkSerialNumbers;
    private String query;
    private Integer from;
    private String cursor;
    private Integer size;
    private String orderBy;
    private String direction;
    private String secDirection;

}
