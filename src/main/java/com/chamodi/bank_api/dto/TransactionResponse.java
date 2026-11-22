package com.chamodi.bank_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.chamodi.bank_api.domain.Transaction;
import com.chamodi.bank_api.domain.TransactionType;

public record TransactionResponse(Long id, TransactionType type, BigDecimal amount, Instant createdAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getType(),
                transaction.getAmount(), transaction.getCreatedAt());
    }
}