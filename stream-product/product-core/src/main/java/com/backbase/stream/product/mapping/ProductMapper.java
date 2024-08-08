package com.backbase.stream.product.mapping;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.backbase.dbs.arrangement.api.integration.v2.model.PostArrangement;
import com.backbase.dbs.arrangement.api.integration.v2.model.TimeUnit;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItem;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItemBase;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementItemPostRequest;
import com.backbase.dbs.arrangement.api.service.v3.model.ArrangementPutItem;
import com.backbase.stream.legalentity.model.AvailableBalance;
import com.backbase.stream.legalentity.model.BaseProduct;
import com.backbase.stream.legalentity.model.BookedBalance;
import com.backbase.stream.legalentity.model.CreditCard;
import com.backbase.stream.legalentity.model.CreditLimit;
import com.backbase.stream.legalentity.model.CurrentAccount;
import com.backbase.stream.legalentity.model.CurrentInvestment;
import com.backbase.stream.legalentity.model.DebitCard;
import com.backbase.stream.legalentity.model.InterestPaymentFrequencyUnit;
import com.backbase.stream.legalentity.model.InvestmentAccount;
import com.backbase.stream.legalentity.model.LegalEntity;
import com.backbase.stream.legalentity.model.LegalEntityReference;
import com.backbase.stream.legalentity.model.Loan;
import com.backbase.stream.legalentity.model.PrincipalAmount;
import com.backbase.stream.legalentity.model.Product;
import com.backbase.stream.legalentity.model.SavingsAccount;
import com.backbase.stream.legalentity.model.TermDeposit;
import com.backbase.stream.legalentity.model.TermUnit;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
@SuppressWarnings({"squid:S1710"})
public interface ProductMapper {


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = "state.externalStateId", target = ProductMapperConstants.STATE_ID)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, qualifiedByName = "mapInterestPaymentFrequencyUnitToTimeUnit", target = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT)
    PostArrangement toPresentation(Product product);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = "state.externalStateId", target = ProductMapperConstants.EXTERNAL_STATE_ID)
    ArrangementItemPostRequest toPresentation(BaseProduct product);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @InheritConfiguration
    @Mapping(source = "debitCardsItems", target = "debitCards")
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
    PostArrangement toPresentation(CurrentAccount currentAccount);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.DEBIT_CARDS_ITEMS, target = ProductMapperConstants.DEBIT_CARDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
    @InheritConfiguration
    PostArrangement toPresentation(SavingsAccount savingsAccount);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = "debitCard", qualifiedByName = "mapDebitCardNumber", target = ProductMapperConstants.NUMBER)
    @InheritConfiguration
    PostArrangement toPresentation(DebitCard debitCard);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = "creditCard", qualifiedByName = "mapCreditCardNumber", target = ProductMapperConstants.NUMBER)
    @InheritConfiguration
    PostArrangement toPresentation(CreditCard creditCard);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
    @InheritConfiguration
    PostArrangement toPresentation(TermDeposit termDeposit);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = "currentInvestment.amount", target = "currentInvestmentValue")
    @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
    @InheritConfiguration
    PostArrangement toPresentation(InvestmentAccount investmentAccount);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
    @InheritConfiguration
    PostArrangement toPresentation(Loan loan);

    @Mapping(source = ProductMapperConstants.STATE_ID, target = "state.state")
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    ArrangementItem toArrangementItem(PostArrangement arrangementItemPost);

    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    ArrangementPutItem toArrangementItemPut(PostArrangement arrangementItemPost);

    ArrangementItemBase toArrangementItemBase(ArrangementItemPostRequest arrangementItemPost);

    ArrangementItem toArrangementItem(ArrangementItemBase arrangementItemBase);

    ArrangementItem toArrangementItem(ArrangementPutItem arrangementItemPut);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, target = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, qualifiedByName = "mapInterestPaymentFrequencyUnitToTimeUnitV3")
    @Mapping(source = ProductMapperConstants.TERM_UNIT, target = ProductMapperConstants.TERM_UNIT, qualifiedByName = "mapTermUnitToTimeUnitV3")
    ArrangementItem toPresentationWithWeirdSpellingError(Product product);

//    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
//    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
//    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
//    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
//    BaseProduct toBaseProduct(ArrangementItem arrangementItem);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @Mapping(source = ProductMapperConstants.TERM_UNIT, target = ProductMapperConstants.TERM_UNIT, qualifiedByName = "mapTimeUnitV3ToTermUnit")
    @Mapping(source = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, target = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, qualifiedByName = "mapTimeUnitV3ToInterestPaymentFrequencyUnit")
    @InheritConfiguration
    Product mapCustomProduct(ArrangementItem arrangementItem);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    CurrentAccount mapCurrentAccount(ArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @Mapping(source = ProductMapperConstants.TERM_UNIT, target = ProductMapperConstants.TERM_UNIT, qualifiedByName = "mapTimeUnitV3ToTermUnit")
    @Mapping(source = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, target = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, qualifiedByName = "mapTimeUnitV3ToInterestPaymentFrequencyUnit")
    SavingsAccount mapSavingAccount(ArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    DebitCard mapDebitCard(ArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    CreditCard mapCreditCard(ArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @Mapping(source = ProductMapperConstants.TERM_UNIT, target = ProductMapperConstants.TERM_UNIT, qualifiedByName = "mapTimeUnitV3ToTermUnit")
    Loan mapLoan(ArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @Mapping(source = ProductMapperConstants.TERM_UNIT, target = ProductMapperConstants.TERM_UNIT, qualifiedByName = "mapTimeUnitV3ToTermUnit")
    @Mapping(source = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, target = ProductMapperConstants.INTEREST_PAYMENT_FREQUENCY_UNIT, qualifiedByName = "mapTimeUnitV3ToInterestPaymentFrequencyUnit")
    TermDeposit mapTermDeposit(ArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @Mapping(source = "currentInvestmentValue", target = "currentInvestment")
    InvestmentAccount mapInvestmentAccount(ArrangementItem product);


    default List<LegalEntityReference> mapLegalEntities(Set<String> externalIds) {
        if (externalIds == null)
            return null;
        return externalIds.stream().map(id -> new LegalEntityReference(null, id)).toList();
    }

    default Set<String> mapLegalEntitiesIds(List<LegalEntityReference> externalIds) {
        if (externalIds == null)
            return null;
        return externalIds.stream().map(LegalEntityReference::getExternalId).collect(Collectors.toSet());
    }

    default BookedBalance mapBookedBalance(BigDecimal bigDecimal) {
        if (bigDecimal == null)
            return null;
        return new BookedBalance().amount(bigDecimal);
    }

    default AvailableBalance mapAvailable(BigDecimal bigDecimal) {
        if (bigDecimal == null)
            return null;
        return new AvailableBalance().amount(bigDecimal);
    }

    default PrincipalAmount mapPrincipal(BigDecimal bigDecimal) {
        if (bigDecimal == null)
            return null;
        return new PrincipalAmount().amount(bigDecimal);
    }

    default CreditLimit mapCreditLimit(BigDecimal bigDecimal) {
        if (bigDecimal == null)
            return null;
        return new CreditLimit().amount(bigDecimal);
    }

    default CurrentInvestment mapCurrentInvestment(BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            return null;
        }
        return new CurrentInvestment().amount(bigDecimal);
    }

    default LegalEntity mapLegalEntity(String legalEntityId) {
        return new LegalEntity().externalId(legalEntityId);
    }

    default OffsetDateTime map(String s) {
        if (isBlank(s)) {
            return null;
        } else {
            try {
                return OffsetDateTime.parse(s, ProductMapperConstants.formatter);
            } catch (java.time.format.DateTimeParseException e) {
                return OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
            }
        }
    }

    /**
     * Convert Object to BigDecimal.
     *
     * @param value wrapped value
     * @return BigDecimal
     */
    default BigDecimal map(BookedBalance value) {
        if (value != null) {
            return value.getAmount();
        } else {
            return null;
        }
    }

    /**
     * Convert Object to BigDecimal.
     *
     * @param value wrapped value
     * @return BigDecimal
     */
    default BigDecimal map(AvailableBalance value) {
        if (value != null) {
            return value.getAmount();
        } else {
            return null;
        }
    }

    /**
     * Convert Object to BigDecimal.
     *
     * @param value wrapped value
     * @return BigDecimal
     */
    default BigDecimal map(PrincipalAmount value) {
        if (value != null) {
            return value.getAmount();
        } else {
            return null;
        }
    }

    /**
     * Convert Object to BigDecimal.
     *
     * @param value wrapped value
     * @return BigDecimal
     */
    default BigDecimal map(CreditLimit value) {
        if (value != null) {
            return value.getAmount();
        } else {
            return null;
        }
    }

    /**
     * Convert Object to BigDecimal.
     *
     * @param offsetDateTime wrapped value
     * @return BigDecimal
     */
    default String map(OffsetDateTime offsetDateTime) {
        if (offsetDateTime != null) {
            return offsetDateTime.toString();
        } else {
            return null;
        }
    }

    @ValueMappings({
            @ValueMapping(source = "QUARTERLY", target = MappingConstants.NULL),
            @ValueMapping(source = ProductMapperConstants.DAILY, target = ProductMapperConstants.D),
            @ValueMapping(source = ProductMapperConstants.WEEKLY, target = ProductMapperConstants.W),
            @ValueMapping(source = ProductMapperConstants.MONTHLY, target = ProductMapperConstants.M),
            @ValueMapping(source = ProductMapperConstants.YEARLY, target = ProductMapperConstants.Y)
    })
    TimeUnit map(TermUnit termUnit);

    @ValueMappings({
            @ValueMapping(source = "QUARTERLY", target = MappingConstants.NULL),
            @ValueMapping(source = ProductMapperConstants.DAILY, target = ProductMapperConstants.D),
            @ValueMapping(source = ProductMapperConstants.WEEKLY, target = ProductMapperConstants.W),
            @ValueMapping(source = ProductMapperConstants.MONTHLY, target = ProductMapperConstants.M),
            @ValueMapping(source = ProductMapperConstants.YEARLY, target = ProductMapperConstants.Y)
    })
    TimeUnit map(InterestPaymentFrequencyUnit interestPaymentFrequencyUnit);

    @ValueMappings({
            @ValueMapping(source = ProductMapperConstants.D, target = ProductMapperConstants.DAILY),
            @ValueMapping(source = ProductMapperConstants.W, target = ProductMapperConstants.WEEKLY),
            @ValueMapping(source = ProductMapperConstants.M, target = ProductMapperConstants.MONTHLY),
            @ValueMapping(source = ProductMapperConstants.Y, target = ProductMapperConstants.YEARLY)
    })
    TermUnit map(TimeUnit unit);

    default java.util.List<java.lang.String> mapLegalEntityId(java.util.List<com.backbase.stream.legalentity.model.LegalEntityReference> value) {
        if (value != null) {
            return value.stream().map(LegalEntityReference::getExternalId).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    default List<LegalEntityReference> mapLegalEntityReference(List<String> value) {
        if (value != null) {
            return value.stream().map(id -> new LegalEntityReference(null, id)).toList();
        } else {
            return null;
        }
    }

    @ValueMappings({
            @ValueMapping(source = ProductMapperConstants.D, target = ProductMapperConstants.DAILY),
            @ValueMapping(source = ProductMapperConstants.W, target = ProductMapperConstants.WEEKLY),
            @ValueMapping(source = ProductMapperConstants.M, target = ProductMapperConstants.MONTHLY),
            @ValueMapping(source = ProductMapperConstants.Y, target = ProductMapperConstants.YEARLY)
    })
    InterestPaymentFrequencyUnit mapInterestPayment(TimeUnit unit);

    @Named("mapInterestPaymentFrequencyUnitToTimeUnit")
    default TimeUnit mapInterestPaymentFrequencyUnitToTimeUnit(InterestPaymentFrequencyUnit interestPaymentFrequencyUnit) {
        if (interestPaymentFrequencyUnit == null) {
            return null;
        }
        String interestPaymentFrequencyUnitString = interestPaymentFrequencyUnit.getValue().substring(0, 1);
        return TimeUnit.fromValue(interestPaymentFrequencyUnitString);
    }

    @Named("mapInterestPaymentFrequencyUnitToTimeUnitV3")
    default com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit mapInterestPaymentFrequencyUnitToTimeUnitV3(InterestPaymentFrequencyUnit interestPaymentFrequencyUnit) {
        if (interestPaymentFrequencyUnit == null) {
            return null;
        }
        String interestPaymentFrequencyUnitString = interestPaymentFrequencyUnit.getValue().substring(0, 1);
        return com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.fromValue(interestPaymentFrequencyUnitString);
    }

    @Named("mapInterestPaymentFrequencyUnitToTermUnit")
    default TermUnit mapInterestPaymentFrequencyUnitToTermUnit(InterestPaymentFrequencyUnit interestPaymentFrequencyUnit) {
        if (interestPaymentFrequencyUnit == null) {
            return null;
        }
        return TermUnit.fromValue(interestPaymentFrequencyUnit.getValue());
    }

    @Named("mapTimeUnitV3ToTermUnit")
    default TermUnit mapTimeUnitV3ToTermUnit(com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit timeUnit) {
        if (timeUnit == null) {
            return null;
        }
        return switch(timeUnit) {
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.D -> TermUnit.DAILY;
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.M -> TermUnit.MONTHLY;
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.Y -> TermUnit.YEARLY;
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.W -> TermUnit.WEEKLY;
        };
    }

    @Named("mapTimeUnitV3ToInterestPaymentFrequencyUnit")
    default InterestPaymentFrequencyUnit mapTimeUnitV3ToInterestPaymentFrequencyUnit(com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit timeUnit) {
        if (timeUnit == null) {
            return null;
        }
        return switch (timeUnit) {
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.D -> InterestPaymentFrequencyUnit.DAILY;
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.W -> InterestPaymentFrequencyUnit.WEEKLY;
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.M -> InterestPaymentFrequencyUnit.MONTHLY;
            case com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.Y -> InterestPaymentFrequencyUnit.YEARLY;
        };
    }

    @Named("mapTermUnitToTimeUnitV3")
    default com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit mapTermUnitToTimeUnitV3(TermUnit termUnit) {
        if (termUnit == null) {
            return null;
        }
        return switch(termUnit) {
            case TermUnit.DAILY -> com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.D;
            case TermUnit.WEEKLY -> com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.W;
            case TermUnit.MONTHLY -> com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.M;
            case TermUnit.YEARLY -> com.backbase.dbs.arrangement.api.service.v3.model.TimeUnit.Y;
            case TermUnit.QUARTERLY -> null;
        };
    }

    // USER PREFERENCES UPDATE HAS BEEN REMOVED FROM V3 ONWARDS BECAUSE UPDATING USER PREFERENCES
    // IS AN INTERNAL OPERATION
//    @Mapping(source = "userExternalId", target = "userId")
//    AccountUserPreferencesItemPut mapUserPreference(UserPreferences userPreferences);

    @Named("mapDebitCardNumber")
    default String mapDebitCardNumber(DebitCard debitCard) {
        String number = debitCard.getNumber();
        if (isNotBlank(number)) {
            return number;
        }
        return debitCard.getPanSuffix();
    }

    @Named("mapCreditCardNumber")
    default String mapCreditCardNumber(CreditCard creditCard) {
        String number = creditCard.getNumber();
        if (isNotBlank(number)) {
            return number;
        }
        return creditCard.getPanSuffix();
    }
}
