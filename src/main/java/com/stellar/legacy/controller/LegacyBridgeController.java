package com.stellar.legacy.controller;

import com.stellar.legacy.model.IdempotencyRecord;
import com.stellar.legacy.model.LegacyTransferRequest;
import com.stellar.legacy.model.LegacyTransferResponse;
import com.stellar.legacy.repository.IdempotencyRepository;
import com.stellar.legacy.service.StellarTransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/legacy")
public class LegacyBridgeController {

    private final StellarTransactionService transactionService;
    private final IdempotencyRepository idempotencyRepository;

    @Autowired
    public LegacyBridgeController(StellarTransactionService transactionService, IdempotencyRepository idempotencyRepository) {
        this.transactionService = transactionService;
        this.idempotencyRepository = idempotencyRepository;
    }

    @PostMapping("/transfer")
    public ResponseEntity<LegacyTransferResponse> handleTransferRequest(@Valid @RequestBody LegacyTransferRequest request) {
        
        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findById(request.getReferenceId());
        if (existingRecord.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new LegacyTransferResponse(
                    request.getReferenceId(),
                    existingRecord.get().getTransactionHash(),
                    existingRecord.get().getStatus(),
                    "A transaction request with this reference ID already exists."
            ));
        }

        IdempotencyRecord newRecord = new IdempotencyRecord(request.getReferenceId(), "PENDING");
        idempotencyRepository.save(newRecord);

        // Process the transfer asynchronously
        transactionService.submitTransferAsync(
                request.getReferenceId(),
                request.getDestinationAddress(),
                request.getAmount(),
                request.getAssetCode()
        );

        // Construct accepted response
        LegacyTransferResponse response = new LegacyTransferResponse(
                request.getReferenceId(),
                null,
                "ACCEPTED",
                "Transfer request accepted and is being processed asynchronously."
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
