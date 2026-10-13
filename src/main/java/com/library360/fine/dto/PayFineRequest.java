package com.library360.fine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PayFineRequest(
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Payment method is required")
    @Size(max = 20, message = "Payment method must be at most 20 characters")
    String method
) {}
