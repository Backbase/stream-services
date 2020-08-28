package com.backbase.stream.transaction.generator;

import static com.backbase.stream.transaction.utils.CommonHelpers.generateRandomNumberInRange;
import static com.backbase.stream.transaction.utils.CommonHelpers.getRandomFromList;

import com.backbase.dbs.transaction.presentation.service.model.CreditDebitIndicator;
import com.backbase.dbs.transaction.presentation.service.model.TransactionItemPost;
import com.backbase.stream.cursor.model.IngestionCursor;
import com.backbase.stream.transaction.generator.configuration.TransactionGeneratorOptions;
import com.backbase.stream.transaction.utils.CommonHelpers;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.iban4j.Iban;

@Slf4j
public class TransactionGenerator {

    private final TransactionGeneratorOptions transactionGeneratorOptions;
    private final Faker faker;

    public TransactionGenerator(TransactionGeneratorOptions transactionGeneratorOptions) {
        this.transactionGeneratorOptions = transactionGeneratorOptions;
        this.faker = new Faker(new Locale(transactionGeneratorOptions.getDefaultLocale()));

    }

    /**
     * Generate Transactions based on Ingestion Cursor.
     *
     * @param ingestionCursor Ingestion Cursor with Arrangement to generate transactions for.
     * @param min             Minimum number of Transactions to generate
     * @param max             Maximum number of Transactions to generate
     * @return List of generated transactions
     */
    public List<TransactionItemPost> generate(IngestionCursor ingestionCursor, int min, int max) {
        int numberOfTxToGenerate = RandomUtils.nextInt(min, max);
        List<TransactionItemPost> transactionItemPosts = new ArrayList<>();
        for (int i = 0; i < numberOfTxToGenerate; i++) {
            transactionItemPosts.add(generate(ingestionCursor));
        }
        return transactionItemPosts;
    }

    public TransactionItemPost generate(IngestionCursor ingestionCursor) {
        LocalDate bookingDate;
        if (ingestionCursor.getDateFrom() != null) {
            LocalDate lastDate = ingestionCursor.getDateFrom();
            LocalDate localDate = LocalDate.now();

            long daysBetween;
            if (lastDate.isAfter(localDate)) {
                daysBetween = ChronoUnit.DAYS.between(localDate, lastDate);
            } else {
                daysBetween = ChronoUnit.DAYS.between(lastDate, localDate);
            }
            bookingDate = lastDate.minusDays(CommonHelpers.generateRandomNumberInRange(0, (int) daysBetween));
        } else {
            bookingDate = LocalDateTime.now().minusDays(generateRandomNumberInRange(365, 365 + 365)).toLocalDate();
        }

        OffsetDateTime offsetDateTime = bookingDate.atTime(OffsetTime.now());

        CreditDebitIndicator isCredit = CommonHelpers.getRandomFromEnumValues(CreditDebitIndicator.values());

        String randomCategory = isCredit.equals(CreditDebitIndicator.CRDT)
            ? getRandomFromList(transactionGeneratorOptions.getCreditRetailCategories())
            : getRandomFromList(transactionGeneratorOptions.getDebitRetailCategories());

        TransactionItemPost transactionItemPost = new TransactionItemPost()
            .externalId(UUID.randomUUID().toString())
            .arrangementId(ingestionCursor.getArrangementId())
            .externalArrangementId(ingestionCursor.getExternalArrangementId())
            .reference(faker.lorem().characters(10))
            .description(faker.lorem().sentence().replace(".", ""))
            .bookingDate(offsetDateTime.toLocalDate())
            .valueDate(offsetDateTime.toLocalDate())
            .typeGroup(getRandomFromList(transactionGeneratorOptions.getTypeGroups()))
            .type(getRandomFromList(transactionGeneratorOptions.getTransactionTypes()))
            .category(randomCategory)

            .transactionAmountCurrency(CommonHelpers.generateRandomAmountInRange(transactionGeneratorOptions.getCurrency(), 100L, 9999L))
            .creditDebitIndicator(isCredit)
            .counterPartyName(faker.name().fullName())
            .counterPartyAccountNumber(Iban.random().toString())
            .counterPartyBIC(faker.finance().bic())
            .counterPartyCountry(faker.address().countryCode())
            .counterPartyBankName(faker.company().name());

        log.info("Creating random transaction for ingestion cursor: {} -> {} with date: {}",
            ingestionCursor.getExternalArrangementId(),
            transactionItemPost.getExternalId(),
            transactionItemPost.getBookingDate());
        return transactionItemPost;
    }
}
