package com.stellar.legacy.service;

import com.stellar.legacy.model.IdempotencyRecord;
import com.stellar.legacy.repository.IdempotencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.stellar.sdk.Asset;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.SubmitTransactionTimeoutResponseException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Service
public class StellarTransactionService {

    private static final Logger logger = Logger.getLogger(StellarTransactionService.class.getName());

    private final Server server;
    private final Network network;
    private final IdempotencyRepository idempotencyRepository;

    @Value("${stellar.account.secret}")
    private String sourceSecret;

    public StellarTransactionService(Server server, Network network, IdempotencyRepository idempotencyRepository) {
        this.server = server;
        this.network = network;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Async
    @Retryable(value = { SubmitTransactionTimeoutResponseException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public CompletableFuture<String> submitTransferAsync(String referenceId, String destinationAddress, BigDecimal amount, String assetCode) {
        logger.info("Initiating async transfer for reference: " + referenceId);
        try {
            KeyPair sourceKeyPair = KeyPair.fromSecretSeed(sourceSecret);
            KeyPair destinationKeyPair = KeyPair.fromAccountId(destinationAddress);

            AccountResponse sourceAccount = server.accounts().account(sourceKeyPair.getAccountId());

            Asset asset = new org.stellar.sdk.AssetTypeNative();

            PaymentOperation paymentOperation = new PaymentOperation.Builder(
                    destinationKeyPair.getAccountId(), 
                    asset, 
                    amount.toPlainString()
            ).build();

            Transaction transaction = new Transaction.Builder(sourceAccount, network)
                    .addOperation(paymentOperation)
                    .addMemo(org.stellar.sdk.Memo.text("Ref:" + referenceId))
                    .setTimeout(180)
                    .setBaseFee(org.stellar.sdk.AbstractTransaction.MIN_BASE_FEE)
                    .build();

            transaction.sign(sourceKeyPair);

            SubmitTransactionResponse response = server.submitTransaction(transaction);

            if (response.isSuccess()) {
                logger.info("Transaction successful! Hash: " + response.getHash());
                updateIdempotencyStatus(referenceId, "SUCCESS", response.getHash());
                return CompletableFuture.completedFuture(response.getHash());
            } else {
                String error = response.getExtras() != null && response.getExtras().getResultCodes() != null 
                        ? response.getExtras().getResultCodes().getTransactionResultCode() : "Unknown";
                logger.severe("Transaction failed: " + error);
                updateIdempotencyStatus(referenceId, "FAILED", null);
                CompletableFuture<String> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Transaction failed. Code: " + error));
                return future;
            }
        } catch (Exception e) {
            logger.severe("Transaction execution failed: " + e.getMessage());
            updateIdempotencyStatus(referenceId, "FAILED", null);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private void updateIdempotencyStatus(String referenceId, String status, String hash) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRepository.findById(referenceId);
        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            record.setStatus(status);
            if (hash != null) {
                record.setTransactionHash(hash);
            }
            idempotencyRepository.save(record);
        }
    }
}
