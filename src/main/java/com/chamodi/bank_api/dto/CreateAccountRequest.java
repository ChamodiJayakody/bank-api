package com.chamodi.bank_api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 20) String accountNumber,
        @NotBlank @Size(max = 100) String ownerName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal openingBalance
) {
}