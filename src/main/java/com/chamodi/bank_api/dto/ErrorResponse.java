package com.chamodi.bank_api.dto;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message) {
}