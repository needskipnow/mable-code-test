package com.mable.codetest;

import com.mable.codetest.BatchProcessor.TransferRequest;

import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

//import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchProcessorTest {
    static final Long FROM_ACCOUNT_NUMBER = 1234567981234568l;
    static final Long TO_ACCOUNT_NUMBER = 9876543219876548l;

    @Test
    public void shouldTransferWithSufficientFunds() {
        // setup
        double fromAccBalance = 100.5;
        double toAccBalance = 48.0;
        double transferAmount = 80.0;
        BatchProcessor processor = new BatchProcessor("a.csv", "b.csv", "output-dir");
        Map<Long, Double> accountBalances = new HashMap<>(of(FROM_ACCOUNT_NUMBER, fromAccBalance, TO_ACCOUNT_NUMBER, toAccBalance));

        TransferRequest transferRequest = new TransferRequest(FROM_ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, transferAmount);

        // execute
        var result = processor.attemptTransfer(transferRequest, accountBalances);

        // verify
        assertEquals(BatchProcessor.Outcome.COMPLETED, result.outcome());
        assertEquals(result.fromAccBalanceUpdated(), fromAccBalance - transferAmount);
        assertEquals(result.toAccBalanceUpdated(), toAccBalance + transferAmount);
    }

    @Test
    public void shouldAbortTransferOnInsufficientFunds() {
        // setup
        double fromAccBalance = 100.5;
        double toAccBalance = 48.0;
        double transferAmount = 180.0;
        BatchProcessor processor = new BatchProcessor("a.csv", "b.csv", "output-dir");
        Map<Long, Double> accountBalances = new HashMap<>(of(FROM_ACCOUNT_NUMBER, fromAccBalance, TO_ACCOUNT_NUMBER, toAccBalance));

        TransferRequest transferRequest = new TransferRequest(FROM_ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, transferAmount);

        // execute
        var result = processor.attemptTransfer(transferRequest, accountBalances);

        // verify
        assertEquals(BatchProcessor.Outcome.DENIED, result.outcome());
        assertEquals(result.fromAccBalanceUpdated(), fromAccBalance);
        assertEquals(result.toAccBalanceUpdated(), toAccBalance);
    }
}