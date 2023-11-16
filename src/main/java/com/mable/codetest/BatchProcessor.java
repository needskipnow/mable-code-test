package com.mable.codetest;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.file.Path.of;

@Slf4j
public class BatchProcessor {

    static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'hh-mm-ss");
    enum Outcome {COMPLETED, DENIED}
    record TransferRequest(Long fromAccount, Long toAccount, double amount) {}

    record TransferResult(TransferRequest request, Outcome outcome, double fromAccBalanceUpdated, double toAccBalanceUpdated, LocalDateTime timestamp) {
        public static String toCsvLine(TransferResult result) {
            return format("%s,%s,%s,%s,%s",
                    result.request.fromAccount(),
                    result.request.toAccount(),
                    result.request.amount(),
                    result.outcome.name(),
                    result.timestamp.atOffset(ZoneOffset.UTC).format(dtf));
        }
    }

    private final String accountBalancesCsvFilePath;
    private final String transferRequestCsvFilePath;
    private final String reportsDir;

    public BatchProcessor(String accountBalanceFilePath, String transferRequestFilePath, String outputDir) {
        this.accountBalancesCsvFilePath = accountBalanceFilePath;
        this.transferRequestCsvFilePath = transferRequestFilePath;
        this.reportsDir = outputDir;
    }

    public void process() {
        log.info("============== START BATCH ===================");

        final Map<Long, Double> accountBalances = loadAccountBalances(accountBalancesCsvFilePath);
        final List<TransferRequest> transferRequests = loadTransferRequests(transferRequestCsvFilePath);

        final List<TransferResult> result = transferRequests.stream()
                .map(transferRequest -> attemptTransfer(transferRequest, accountBalances))
                .toList();
        printResult(result, format("%s/mable_transfer_run_%s.csv", reportsDir, LocalDateTime.now(ZoneId.systemDefault()).format(dtf)));
        printAccountBalances(accountBalances, format("%s/mable_acc_balance_post_run_%s.csv", reportsDir, LocalDateTime.now(ZoneId.systemDefault()).format(dtf)));

        log.info("============== END BATCH ===================");
    }

    private void printResult(List<TransferResult> result, String filePath) {
        try(PrintWriter pw = new PrintWriter(filePath.replace(":","-"))) {
            pw.println("FROM ACCOUNT,TO ACCOUNT,AMOUNT,STATUS,TIMESTAMP");
            result.stream().map(TransferResult::toCsvLine).forEach(pw::println);
        } catch (IOException e) {
            log.error("============== failed to save CSV file {} ===================", filePath, e);
        }
    }

    private void printAccountBalances(final Map<Long, Double> accountBalances, String filePath) {
        try(PrintWriter pw = new PrintWriter(filePath.replace(":","-"))) {
            pw.println("ACCOUNT NUMBER,BALANCE");
            pw.println(accountBalances.entrySet().stream()
                    .map(entry -> format("%s,%s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining("\n")));
        } catch (IOException e) {
            log.error("============== failed to save CSV file {} ===================", filePath, e);
        }
    }

    protected TransferResult attemptTransfer(final TransferRequest request, final Map<Long, Double> accountBalances) {
        var fromAccBalance = accountBalances.get(request.fromAccount);
        var toAccBalance = accountBalances.get(request.toAccount);
        if (request.amount() > fromAccBalance) {
            log.info("============== TRANSFER ABORTED: Insufficient funds on account {} ===================", request.fromAccount());
            return new TransferResult(request, Outcome.DENIED, fromAccBalance, toAccBalance, LocalDateTime.now());
        }

        var fromAccBalanceUpdated = fromAccBalance - request.amount;
        var toAccBalanceUpdated = toAccBalance + request.amount;

        accountBalances.put(request.fromAccount, fromAccBalanceUpdated);
        accountBalances.put(request.toAccount, toAccBalanceUpdated);

        log.info("============== TRANSFER COMPLETED from account: {}, to account {}, amount: ${} ===========", request.fromAccount(), request.toAccount(), request.amount());
        return new TransferResult(request, Outcome.COMPLETED, fromAccBalanceUpdated, toAccBalanceUpdated, LocalDateTime.now(ZoneId.systemDefault()));
    }

    private Map<Long, Double> loadAccountBalances(String filePath) {
        Map<Long, Double> accountBalances = new HashMap<>();
        try (var reader = Files.newBufferedReader(of(BankApp.class.getClassLoader().getResource(filePath).toURI()))) {
            var line = reader.readLine();
            while (line != null) {
                String[] values = line.split(",");
                accountBalances.put(Long.valueOf(values[0]), Double.valueOf(values[1]));
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        return accountBalances;
    }

    private List<TransferRequest> loadTransferRequests(String filePath) {
        List<TransferRequest> transferRequests = new ArrayList<>();
        try (var reader = Files.newBufferedReader(of(BankApp.class.getClassLoader().getResource(filePath).toURI()))) {
            var line = reader.readLine();
            while (line != null) {
                String[] values = line.split(",");
                transferRequests.add(new TransferRequest(Long.valueOf(values[0]), Long.valueOf(values[1]), Double.valueOf(values[2])));
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        return transferRequests;
    }
}
