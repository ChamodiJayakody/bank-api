package com.chamodi.bank_api.exception;

public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException(String accountNumber) {
        super("Account number " + accountNumber + " already exists");
    }
}