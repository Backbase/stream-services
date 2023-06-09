package com.backbase.stream.product.mapping;

import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemBase;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPost;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountArrangementItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.AccountUserPreferencesItemPut;
import com.backbase.dbs.arrangement.api.service.v2.model.TimeUnit;
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
import com.backbase.stream.legalentity.model.UserPreferences;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import org.springframework.util.StringUtils;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
@SuppressWarnings({"squid:S1710"})
public interface ProductMapper {

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(source = "state.externalStateId", target = ProductMapperConstants.EXTERNAL_STATE_ID)
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  AccountArrangementItemPost toPresentation(Product product);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(source = "state.externalStateId", target = ProductMapperConstants.EXTERNAL_STATE_ID)
  AccountArrangementItemPost toPresentation(BaseProduct product);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @InheritConfiguration
  @Mapping(source = "debitCardsItems", target = "debitCards")
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
  AccountArrangementItemPost toPresentation(CurrentAccount currentAccount);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(
      source = ProductMapperConstants.DEBIT_CARDS_ITEMS,
      target = ProductMapperConstants.DEBIT_CARDS)
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
  @InheritConfiguration
  AccountArrangementItemPost toPresentation(SavingsAccount savingsAccount);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  @Mapping(
      source = "debitCard",
      qualifiedByName = "mapDebitCardNumber",
      target = ProductMapperConstants.NUMBER)
  @InheritConfiguration
  AccountArrangementItemPost toPresentation(DebitCard debitCard);

  /**
   * @param creditCard
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  @Mapping(
      source = "creditCard",
      qualifiedByName = "mapCreditCardNumber",
      target = ProductMapperConstants.NUMBER)
  @InheritConfiguration
  AccountArrangementItemPost toPresentation(CreditCard creditCard);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
  @InheritConfiguration
  AccountArrangementItemPost toPresentation(TermDeposit termDeposit);

  /**
   * @param investmentAccount
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(source = "currentInvestment.amount", target = "currentInvestmentValue")
  @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
  @InheritConfiguration
  AccountArrangementItemPost toPresentation(InvestmentAccount investmentAccount);

  /**
   * @param loan
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  @Mapping(
      source = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_PRODUCT_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITIES,
      target = ProductMapperConstants.EXTERNAL_LEGAL_ENTITY_IDS)
  @Mapping(
      source = ProductMapperConstants.ACCOUNT_HOLDER_NAME,
      target = ProductMapperConstants.ACCOUNT_HOLDER_NAMES)
  @Mapping(source = ProductMapperConstants.PAN_SUFFIX, target = ProductMapperConstants.NUMBER)
  @InheritConfiguration
  AccountArrangementItemPost toPresentation(Loan loan);

  /**
   * @param arrangementItemPost
   * @return
   */
  AccountArrangementItem toArrangementItem(AccountArrangementItemPost arrangementItemPost);

  /**
   * @param arrangementItemPost
   * @return
   */
  AccountArrangementItemPut toArrangementItemPut(AccountArrangementItemPost arrangementItemPost);

  AccountArrangementItemBase toArrangementItemBase(AccountArrangementItemPost arrangementItemPost);

  /**
   * @param arrangementItemPost
   * @return
   */
  AccountArrangementItem toArrangementItem(AccountArrangementItemBase arrangementItemPost);

  /**
   * @param product
   * @return
   */
  //
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ID,
      target = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID)
  AccountArrangementItem toPresentationWithWeirdSpellingError(Product product);

  //    @Mapping(source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID, target =
  // ProductMapperConstants.EXTERNAL_ID)
  //    @Mapping(source = ProductMapperConstants.EXTERNAL_PRODUCT_ID, target =
  // ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  //    @Mapping(source = ProductMapperConstants.LEGAL_ENTITY_IDS, target =
  // ProductMapperConstants.LEGAL_ENTITIES)
  //    @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  //    BaseProduct toBaseProduct(ArrangementItem arrangementItem);

  /**
   * @param arrangementItem
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  @InheritConfiguration
  Product mapCustomProduct(AccountArrangementItem arrangementItem);

  /**
   * @param product
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  CurrentAccount mapCurrentAccount(AccountArrangementItem product);

  /**
   * @param product
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  SavingsAccount mapSavingAccount(AccountArrangementItem product);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  DebitCard mapDebitCard(AccountArrangementItem product);

  /**
   * @param product
   * @return
   */
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  CreditCard mapCreditCard(AccountArrangementItem product);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  Loan mapLoan(AccountArrangementItem product);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  TermDeposit mapTermDeposit(AccountArrangementItem product);

  @Mapping(
      source = ProductMapperConstants.EXTERNAL_ARRANGEMENT_ID,
      target = ProductMapperConstants.EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.EXTERNAL_PRODUCT_ID,
      target = ProductMapperConstants.PRODUCT_TYPE_EXTERNAL_ID)
  @Mapping(
      source = ProductMapperConstants.LEGAL_ENTITY_IDS,
      target = ProductMapperConstants.LEGAL_ENTITIES)
  @Mapping(source = ProductMapperConstants.ID, target = ProductMapperConstants.INTERNAL_ID)
  @Mapping(source = "currentInvestmentValue", target = "currentInvestment")
  InvestmentAccount mapInvestmentAccount(AccountArrangementItem product);

  /**
   * @param bigDecimal
   * @return
   */
  default BookedBalance mapBookedBalance(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    return new BookedBalance().amount(bigDecimal);
  }

  /**
   * @param bigDecimal
   * @return
   */
  default AvailableBalance mapAvailable(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    return new AvailableBalance().amount(bigDecimal);
  }

  /**
   * @param bigDecimal
   * @return
   */
  default PrincipalAmount mapPrincipal(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    return new PrincipalAmount().amount(bigDecimal);
  }

  /**
   * @param bigDecimal
   * @return
   */
  default CreditLimit mapCreditLimit(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    return new CreditLimit().amount(bigDecimal);
  }

  /**
   * @param bigDecimal
   * @return
   */
  default CurrentInvestment mapCurrentInvestment(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    return new CurrentInvestment().amount(bigDecimal);
  }

  /**
   * @param legalEntityId
   * @return
   */
  default LegalEntity mapLegalEntity(String legalEntityId) {
    return new LegalEntity().externalId(legalEntityId);
  }

  /**
   * @param s
   * @return
   */
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

  /**
   * @param value
   * @return
   */
  default java.util.List<java.lang.String> mapLegalEntityId(
      java.util.List<com.backbase.stream.legalentity.model.LegalEntityReference> value) {
    if (value != null) {
      return value.stream().map(LegalEntityReference::getExternalId).collect(Collectors.toList());
    } else {
      return null;
    }
  }

  /**
   * @param value
   * @return
   */
  default List<LegalEntityReference> mapLegalEntityReference(List<String> value) {
    if (value != null) {
      return value.stream()
          .map(id -> new LegalEntityReference().externalId(id))
          .collect(Collectors.toList());
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

  @Named("mapDebitCardNumber")
  default String mapDebitCardNumber(DebitCard debitCard) {
    if (StringUtils.hasText(debitCard.getNumber())) {
      return debitCard.getNumber();
    }
    return debitCard.getPanSuffix();
  }

  @Named("mapCreditCardNumber")
  default String mapCreditCardNumber(CreditCard creditCard) {
    if (StringUtils.hasText(creditCard.getNumber())) {
      return creditCard.getNumber();
    }
    return creditCard.getPanSuffix();
  }
}
