package com.chamodi.bank_api.service;

import java.math.BigDecimal;

import com.chamodi.bank_api.domain.Account;

public record TransferResult(Account from, Account to, BigDecimal amount) {
}