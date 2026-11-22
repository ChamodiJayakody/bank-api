package com.chamodi.bank_api.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId) {
        super("Insufficient funds in account " + accountId);
    }
}