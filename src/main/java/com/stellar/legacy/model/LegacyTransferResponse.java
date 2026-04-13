package com.stellar.legacy.model;

public class LegacyTransferResponse {

    private String referenceId;
    private String transactionHash;
    private String status;
    private String message;

    public LegacyTransferResponse(String referenceId, String transactionHash, String status, String message) {
        this.referenceId = referenceId;
        this.transactionHash = transactionHash;
        this.status = status;
        this.message = message;
    }

    // Getters and Setters

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
