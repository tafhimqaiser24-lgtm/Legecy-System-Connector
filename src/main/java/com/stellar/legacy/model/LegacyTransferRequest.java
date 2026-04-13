package com.stellar.legacy.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class LegacyTransferRequest {

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @Positive(message = "Amount must be strictly positive")
    private BigDecimal amount;

    private String assetCode; // Optional: e.g., USD, EUR. If null, use XLM

    @NotBlank(message = "Reference ID is required")
    private String referenceId; // Legacy system's internal tracking ID

    // Getters and Setters

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
