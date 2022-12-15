package com.backbase.stream.product.mapping;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemBase;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPost;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountUserPreferencesItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.TimeUnit;
import com.backbase.stream.legalentity.model.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import org.springframework.util.StringUtils;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
@SuppressWarnings({"squid:S1710"})
public interface ProductMapper {


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = "state.externalStateId", target = ProductMapperConstants.EXTERNAL_STATE_ID)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    AccountArrangementItemPost toPresentation(Product product);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = "state.externalStateId", target = ProductMapperConstants.EXTERNAL_STATE_ID)
    AccountArrangementItemPost toPresentation(BaseProduct product);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @InheritConfiguration
    @Mapping(source = "debitCardsItems", target = "debitCards")
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    AccountArrangementItemPost toPresentation(CurrentAccount currentAccount);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.DEBIT_CARDS_ITEMS, target = ProductMapperConstants.DEBIT_CARDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @InheritConfiguration
    AccountArrangementItemPost toPresentation(SavingsAccount savingsAccount);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @InheritConfiguration
    AccountArrangementItemPost toPresentation(DebitCard debitCard);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @InheritConfiguration
    AccountArrangementItemPost toPresentation(CreditCard creditCard);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @InheritConfiguration
    AccountArrangementItemPost toPresentation(TermDeposit termDeposit);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @InheritConfiguration
    AccountArrangementItemPost toPresentation(InvestmentAccount investmentAccount);


    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    @Mapping(source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITIES, target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
    @Mapping(source = ProductMapperConstants.ACCOUNT_HOLDER_NAME, target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
    @InheritConfiguration
    AccountArrangementItemPost toPresentation(Loan loan);

    AccountArrangementItem toArrangementItem(AccountArrangementItemPost arrangementItemPost);

    AccountArrangementItemPut toArrangementItemPut(AccountArrangementItemPost arrangementItemPost);

    AccountArrangementItemBase toArrangementItemBase(AccountArrangementItemPost arrangementItemPost);

    AccountArrangementItem toArrangementItem(AccountArrangementItemBase arrangementItemPost);

    //
    @Mapping(source = ProductMapperConstants.EXTERNAL_ID, target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
    AccountArrangementItem toPresentationWithWeirdSpellingError(Product product);

//    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
//    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
//    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
//    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
//    BaseProduct toBaseProduct(ArrangementItem arrangementItem);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @InheritConfiguration
    Product mapCustomProduct(AccountArrangementItem arrangementItem);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    CurrentAccount mapCurrentAccount(AccountArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    SavingsAccount mapSavingAccount(AccountArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    DebitCard mapDebitCard(AccountArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    CreditCard mapCreditCard(AccountArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    Loan mapLoan(AccountArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    TermDeposit mapTermDeposit(AccountArrangementItem product);

    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target = ProductMapperConstants.EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target = ProductMapperConstants.LEGAL_ENTITIES)
    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
    @Mapping(source = "currentInvestmentValue", target = "currentInvestment")
    InvestmentAccount mapInvestmentAccount(AccountArrangementItem product);


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
        if (StringUtils.isEmpty(s)) {
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
            return value.stream().map(id -> new LegalEntityReference().externalId(id)).collect(Collectors.toList());
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

    @Mapping(source = "userExternalId", target = "userId")
    AccountUserPreferencesItemPut mapUserPreference(UserPreferences userPreferences);

}
