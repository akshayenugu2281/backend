package com.library360.fine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiveFineRequest(
    @NotBlank(message = "Waive reason is required")
    @Size(max = 255, message = "Waive reason must be at most 255 characters")
    String reason
) {}
