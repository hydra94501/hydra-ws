package com.hydra.websocket.exceptions;

public class PaymentException extends RuntimeException {
    private final String errorCode;
    
    public PaymentException(String message) {
        this("PAYMENT_ERROR", message);
    }
    
    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}