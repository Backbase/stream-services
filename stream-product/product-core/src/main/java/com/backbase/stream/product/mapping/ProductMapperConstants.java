package com.backbase.stream.product.mapping;

import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
class ProductMapperConstants {

    private static final String YYYY_MM_DD_T_HH_MM_SS_SSSXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
    static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(ProductMapperConstants.YYYY_MM_DD_T_HH_MM_SS_SSSXX);
    static final String DAILY = "DAILY";
    static final String WEEKLY = "WEEKLY";
    static final String MONTHLY = "MONTHLY";
    static final String QUARTERLY = "QUARTERLY";
    static final String YEARLY = "YEARLY";
    static final String D = "D";
    static final String W = "W";
    static final String M = "M";
    static final String Y = "Y";
    static final String ACCOUNT_HOLDER_NAME = "accountHolderName";
    static final String ACCOUNT_HOLDER_NAMES = "accountHolderNames";
    static final String EXTERNAL_ARRANGEMENT_ID = "externalArrangementId";
    static final String EXTERNAL_PRODUCT_ID = "externalProductId";
    static final String EXTERNAL_LEGAL_ENTITIES = "externalLegalEntities";
    static final String EXTERNAL_ID = "externalId";
    static final String PRODUCT_TYPE_EXTERNAL_ID = "productTypeExternalId";
    static final String LEGAL_ENTITIES = "legalEntities";
    static final String LEGAL_ENTITY_IDS = "legalEntityIds";
    static final String ID = "id";
    static final String EXTERNAL_STATE_ID = "externalStateId";
    static final String INTERNAL_ID = "internalId";
    static final String DEBIT_CARDS_ITEMS = "debitCardsItems";
    static final String DEBIT_CARDS = "debitCards";
    static final String NUMBER = "number";
    static final String PAN_SUFFIX = "panSuffix";
    static final String PRODUCT_ID = "productId";
    static final String STATE_ID = "stateId";
    static final String PARENT_ID = "parentId";
    static final String EXTERNAL_PARENT_ID = "externalParentId";
    static final String INTEREST_PAYMENT_FREQUENCY_UNIT = "interestPaymentFrequencyUnit";
    static final String TERM_UNIT = "termUnit";
    static final String ACCOUNT_HOLDER_COUNTRY = "accountHolderCountry";
    static final String ACCOUNT_HOLDER_ADDRESS_1 =  "accountHolderAddressLine1";
    static final String ACCOUNT_HOLDER_ADDRESS_2 =  "accountHolderAddressLine2";
    static final String ACCOUNT_HOLDER_POST_CODE = "postCode";
    static final String ACCOUNT_HOLDER_CITY = "town";
    static final String ACCOUNT_HOLDER_COUNTRY_SUBDIVISION = "countrySubDivision";
    static final String ACCOUNT_HOLDER_STREET_NAME = "accountHolderStreetName";
    static final String ACCOUNT_HOLDER_WITH_NAMES = "accountHolder.names";
    static final String ACCOUNT_HOLDER_WITH_NAME = "accountHolder.name";
    static final String ACCOUNT_HOLDER_WITH_ADDRESS_1 = "accountHolder.address.addressLine1";
    static final String ACCOUNT_HOLDER_WITH_ADDRESS_2 = "accountHolder.address.addressLine2";
    static final String ACCOUNT_HOLDER_WITH_POST_CODE = "accountHolder.address.postCode";
    static final String ACCOUNT_HOLDER_WITH_CITY = "accountHolder.address.city";
    static final String ACCOUNT_HOLDER_WITH_COUNTRY_SUBDIVISION = "accountHolder.address.countrySubDivision";
    static final String ACCOUNT_HOLDER_WITH_COUNTRY = "accountHolder.address.country";
    static final String ACCOUNT_HOLDER_WITH_STREET_NAME = "accountHolder.address.streetName";

}
