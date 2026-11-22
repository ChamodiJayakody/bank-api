package com.chamodi.bank_api.dto;

import java.math.BigDecimal;

import com.chamodi.bank_api.service.TransferResult;

public record TransferResponse(Long fromAccountId, Long toAccountId, BigDecimal amount,
                               BigDecimal fromBalance, BigDecimal toBalance) {

    public static TransferResponse from(TransferResult result) {
        return new TransferResponse(result.from().getId(), result.to().getId(), result.amount(),
                result.from().getBalance(), result.to().getBalance());
    }
}