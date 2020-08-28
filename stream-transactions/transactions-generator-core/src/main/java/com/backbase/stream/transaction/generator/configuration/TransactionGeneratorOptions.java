package com.backbase.stream.transaction.generator.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import static java.util.Arrays.asList;

@ConfigurationProperties("generator.transaction")
@Data
public class TransactionGeneratorOptions {

    private List<String> typeGroups = Defaults.TRANSACTION_TYPE_GROUPS;
    private List<String> transactionTypes = Defaults.TRANSACTION_TYPES;
    private List<String> debitRetailCategories = Defaults.DEBIT_RETAIL_CATEGORIES;
    private List<String> creditRetailCategories = Defaults.CREDIT_RETAIL_CATEGORIES;
    private String currency = Defaults.EUR_CURRENCY;

    private double randomDebitAmountMinimum = Defaults.randomDebitAmountMinimum;
    private double randomDebitAmountMaximum = Defaults.randomDebitAmountMaximum;
    private double randomCreditAmountMinimum = Defaults.randomCreditAmountMinimum;
    private double randomCreditAmountMaximum = Defaults.randomCreditAmountMaximum;
    private double randomFeeAmountMinimum = Defaults.randomFeeAmountMinimum;
    private double randomFeeAmountMaximum = Defaults.randomFeeAmountMaximum;

    private int numberOfDebitTransactionsPerMonth = Defaults.numberOfDebitTransactionsPerMonth;
    private int numberOfCreditTransactionsPerMonth = Defaults.numberOfCreditTransactionsPerMonth;

    private int numberOfMonthsToGenerate = Defaults.numberOfMonthsToGenerate;

    private String defaultLocale = Defaults.defaultLocale;

    public static class Defaults {

        private static final double randomDebitAmountMinimum = 3000;
        private static final double  randomDebitAmountMaximum = 3000;
        private static final double  randomCreditAmountMinimum = 0.25;
        private static final double  randomCreditAmountMaximum = 1500;
        private static final double  randomFeeAmountMinimum = 0.01;
        private static final double  randomFeeAmountMaximum = 2.99;

        private static final int numberOfDebitTransactionsPerMonth = 1;
        private static final int numberOfCreditTransactionsPerMonth = 50;

        private static final int numberOfMonthsToGenerate = 1;


        private static final String EUR_CURRENCY = "EUR";


        private static final List<String> TRANSACTION_TYPE_GROUPS = asList(
            "Payment",
            "Withdrawal",
            "Loans",
            "Fees"
        );
        private static final List<String> TRANSACTION_TYPES = asList(
            "ATM",
            "ACH",
            "Bill Payment",
            "Cash",
            "Cheques",
            "Credit/Debit Card",
            "Check",
            "Deposit",
            "Fee",
            "POS",
            "Withdrawal");
        public static String defaultLocale = "NL";

        private static List<String> DEBIT_RETAIL_CATEGORIES = asList(
            "Food Drinks",
            "Transportation",
            "Home",
            "Health Beauty",
            "Shopping",
            "Bills Utilities",
            "Hobbies Entertainment",
            "Transfers",
            "Uncategorised",
            "Car",
            "Beauty",
            "Health Fitness",
            "Mortgage",
            "Rent",
            "Public Transport",
            "Internet",
            "Mobile Phone",
            "Utilities",
            "Alcohol Bars",
            "Fast Food",
            "Groceries",
            "Restaurants",
            "Clothing",
            "Electronics"
        );
        private static List<String> CREDIT_RETAIL_CATEGORIES = asList(
            "Income",
            "Other Income",
            "Bonus",
            "Salary/Wages",
            "Interest Income",
            "Rental Income"
        );
    }
}
