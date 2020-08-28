package com.backbase.stream.transactions;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TransactionServiceTests extends AbstractTransactionServiceTests {

    @Test
    public void testLastTransaction() {
        transactionService.getLatestTransactions("savings-account-another-0one", 1)
            .blockLast();
    }

}
