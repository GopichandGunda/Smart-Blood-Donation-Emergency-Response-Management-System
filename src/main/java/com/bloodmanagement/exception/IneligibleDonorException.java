package com.bloodmanagement.exception;

public class IneligibleDonorException extends RuntimeException {
    public IneligibleDonorException(String message) {
        super(message);
    }
}
