package com.stellar.legacy.exception;

import com.stellar.legacy.model.LegacyErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.stellar.sdk.responses.SubmitTransactionUnknownResponseException;
import org.stellar.sdk.responses.SubmitTransactionTimeoutResponseException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LegacyErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        LegacyErrorResponse response = new LegacyErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<LegacyErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        LegacyErrorResponse response = new LegacyErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LegacyErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            sb.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; ")
        );
        LegacyErrorResponse response = new LegacyErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                sb.toString(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Stellar SDK Exceptions Handling
    @ExceptionHandler(SubmitTransactionTimeoutResponseException.class)
    public ResponseEntity<LegacyErrorResponse> handleStellarTimeoutException(SubmitTransactionTimeoutResponseException ex, HttpServletRequest request) {
        LegacyErrorResponse response = new LegacyErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "Stellar Network Timeout",
                "Transaction submission timed out. It might have succeeded or failed. Check account status.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(SubmitTransactionUnknownResponseException.class)
    public ResponseEntity<LegacyErrorResponse> handleStellarUnknownException(SubmitTransactionUnknownResponseException ex, HttpServletRequest request) {
        LegacyErrorResponse response = new LegacyErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "Stellar Network Error",
                "Received an unknown response from the Stellar network: " + ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    // Custom Transaction Runtime Exception mapping
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<LegacyErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        // e.g., if we throw RuntimeException over a Tx failed outcome
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String error = "Transaction Execution Failed";
        
        if (ex.getMessage() != null && ex.getMessage().contains("op_low_reserve")) {
            status = HttpStatus.UNPROCESSABLE_ENTITY.value();
            error = "Insufficient Reserve";
        } else if (ex.getMessage() != null && ex.getMessage().contains("tx_bad_seq")) {
            status = HttpStatus.CONFLICT.value();
            error = "Bad Sequence Number";
        }
        
        LegacyErrorResponse response = new LegacyErrorResponse(
                status,
                error,
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.valueOf(status));
    }
}
