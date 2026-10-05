package com.bloodmanagement.exception;

public class InsufficientBloodStockException extends RuntimeException {
    public InsufficientBloodStockException(String message) {
        super(message);
    }
}
