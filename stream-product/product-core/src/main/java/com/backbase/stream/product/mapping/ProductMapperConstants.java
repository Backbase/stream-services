package com.backbase.stream.product.mapping;

import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
class ProductMapperConstants {

    static final String DAILY = "DAILY";
    static final String WEEKLY = "WEEKLY";
    static final String MONTHLY = "MONTHLY";
    static final String YEARLY = "YEARLY";
    static final String D = "D";
    static final String W = "W";
    static final String M = "M";
    static final String Y = "Y";
    static final String ACCOUNT_HOLDER_NAME = "accountHolderName";
    static final String ACCOUNT_HOLDER_NAMES = "accountHolderNames";
    static final String EXTERNAL_ARRANGEMENT_ID = "externalArrangementId";
    static final String EXTERNAL_PRODUCT_ID = "externalProductId";
    static final String EXTERNAL_LEGAL_ENTITY_IDS = "externalLegalEntityIds";
    static final String EXTERNAL_ID = "externalId";
    static final String PRODUCT_TYPE_EXTERNAL_ID = "productTypeExternalId";
    static final String LEGAL_ENTITIES = "legalEntities";
    static final String LEGAL_ENTITY_IDS = "legalEntityIds";
    static final String ID = "id";
    static final String EXTERNAL_STATE_ID = "externalStateId";
    static final String INTERNAL_ID = "internalId";
    static final String DEBIT_CARDS_ITEMS = "debitCardsItems";
    static final String DEBIT_CARDS = "debitCards";
    private static final String YYYY_MM_DD_T_HH_MM_SS_SSSXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
    static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(ProductMapperConstants.YYYY_MM_DD_T_HH_MM_SS_SSSXX);
}
