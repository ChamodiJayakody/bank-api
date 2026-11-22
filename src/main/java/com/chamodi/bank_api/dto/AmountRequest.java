package com.chamodi.bank_api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AmountRequest(
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount
) {
}